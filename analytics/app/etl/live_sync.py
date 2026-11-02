import logging
import pandas as pd
from datetime import datetime
from sqlalchemy import text
from ..db import engine

logger = logging.getLogger("cms_analytics.live_sync")

def sync_operational_order(order_data: dict) -> dict:
    """
    Synchronizes a single operational sales order from Spring Boot directly into the Star-Schema Data Warehouse.
    Updates dim_date, dim_customer, dim_product, dim_employee, and fact_sales_order in real-time.
    """
    order_id = order_data.get("id")
    order_number = order_data.get("orderNumber")
    if not order_number:
        return {"status": "SKIPPED", "reason": "No order number provided"}

    order_date_str = order_data.get("orderDate")
    if order_date_str:
        try:
            dt = pd.to_datetime(order_date_str)
        except Exception:
            dt = datetime.now()
    else:
        dt = datetime.now()

    date_key = int(dt.strftime("%Y%m%d"))
    region = str(order_data.get("region", "NORTH")).upper()
    status = str(order_data.get("status", "INQUIRY")).upper()
    discount_pct = float(order_data.get("discountPercent", 0.0) or 0.0)
    customer_id = order_data.get("customerId", 1)
    customer_name = order_data.get("customerName", "Enterprise Client")
    sales_rep_id = order_data.get("salesRepId", 1)
    sales_rep_name = order_data.get("salesRepName", "Account Executive")

    items = order_data.get("items", [])
    if not items:
        # Fallback if no item details attached
        items = [{
            "productId": 1,
            "productSku": "PROD-GEN-01",
            "productName": "Enterprise Solution",
            "category": "Enterprise Software",
            "quantity": 1,
            "unitPrice": float(order_data.get("subtotalAmount", order_data.get("totalAmount", 10000.0))),
            "unitCost": float(order_data.get("totalCost", 3000.0)),
            "lineTotal": float(order_data.get("subtotalAmount", order_data.get("totalAmount", 10000.0)))
        }]

    with engine.begin() as conn:
        # 1. Ensure dim_date
        date_exists = conn.execute(
            text("SELECT 1 FROM dim_date WHERE date_key = :dk"), {"dk": date_key}
        ).fetchone()
        if not date_exists:
            conn.execute(text("""
                INSERT INTO dim_date (date_key, full_date, day_of_week, day_of_month, month, month_name, quarter, year, is_weekend)
                VALUES (:dk, :fd, :dow, :dom, :m, :mn, :q, :y, :iw)
            """), {
                "dk": date_key,
                "fd": dt.strftime("%Y-%m-%d"),
                "dow": dt.strftime("%A"),
                "dom": dt.day,
                "m": dt.month,
                "mn": dt.strftime("%B"),
                "q": (dt.month - 1) // 3 + 1,
                "y": dt.year,
                "iw": dt.weekday() >= 5
            })

        # 2. Ensure dim_customer
        cust_row = conn.execute(
            text("SELECT customer_key FROM dim_customer WHERE UPPER(company_name) = :cname OR customer_id = :cid LIMIT 1"),
            {"cname": customer_name.upper(), "cid": customer_id}
        ).fetchone()
        if cust_row:
            customer_key = cust_row[0]
            # Update region if needed
            conn.execute(
                text("UPDATE dim_customer SET region = :reg WHERE customer_key = :ck"),
                {"reg": region, "ck": customer_key}
            )
        else:
            max_ck = conn.execute(text("SELECT COALESCE(MAX(customer_key), 0) + 1 FROM dim_customer")).scalar()
            customer_key = int(max_ck)
            conn.execute(text("""
                INSERT INTO dim_customer (customer_id, company_name, region, customer_key)
                VALUES (:cid, :cname, :reg, :ck)
            """), {"cid": customer_id, "cname": customer_name, "reg": region, "ck": customer_key})

        # 3. Ensure dim_employee
        emp_row = conn.execute(
            text("SELECT employee_key FROM dim_employee WHERE UPPER(full_name) = :ename LIMIT 1"),
            {"ename": sales_rep_name.upper()}
        ).fetchone()
        if emp_row:
            employee_key = emp_row[0]
        else:
            max_ek = conn.execute(text("SELECT COALESCE(MAX(employee_key), 0) + 1 FROM dim_employee")).scalar()
            employee_key = int(max_ek)
            conn.execute(text("""
                INSERT INTO dim_employee (employee_code, full_name, employee_key, department_name, job_title)
                VALUES (:ecode, :ename, :ek, 'Sales', 'Account Executive')
            """), {"ecode": f"EMP-{sales_rep_id:03d}", "ename": sales_rep_name, "ek": employee_key})

        # 4. Remove any previous rows for this order in fact_sales_order
        conn.execute(
            text("DELETE FROM fact_sales_order WHERE order_number = :onum OR order_id = :oid"),
            {"onum": order_number, "oid": order_id}
        )

        # 5. Insert each line item into fact_sales_order
        for item in items:
            p_sku = str(item.get("productSku") or f"SKU-{item.get('productId', 1)}")
            p_name = str(item.get("productName") or "Enterprise Solution")
            p_cat = str(item.get("category") or "Enterprise Software")
            unit_price = float(item.get("unitPrice", 0.0) or 0.0)
            unit_cost = float(item.get("unitCost", 0.0) or 0.0)
            qty = int(item.get("quantity", 1) or 1)

            # Ensure dim_product
            prod_row = conn.execute(
                text("SELECT product_key FROM dim_product WHERE UPPER(sku) = :sku OR UPPER(name) = :name LIMIT 1"),
                {"sku": p_sku.upper(), "name": p_name.upper()}
            ).fetchone()
            if prod_row:
                product_key = prod_row[0]
            else:
                max_pk = conn.execute(text("SELECT COALESCE(MAX(product_key), 0) + 1 FROM dim_product")).scalar()
                product_key = int(max_pk)
                conn.execute(text("""
                    INSERT INTO dim_product (sku, name, category, unit_price, unit_cost, product_key)
                    VALUES (:sku, :name, :cat, :up, :uc, :pk)
                """), {"sku": p_sku, "name": p_name, "cat": p_cat, "up": unit_price, "uc": unit_cost, "pk": product_key})

            gross = round(qty * unit_price, 2)
            net = round(gross * (1.0 - (discount_pct / 100.0)), 2)
            cost = round(qty * unit_cost, 2)
            profit = round(net - cost, 2)

            max_fid = conn.execute(text("SELECT COALESCE(MAX(fact_id), 0) + 1 FROM fact_sales_order")).scalar()
            fact_id = int(max_fid)

            conn.execute(text("""
                INSERT INTO fact_sales_order (
                    order_number, date_key, customer_key, employee_key, product_key,
                    region, order_status, quantity, unit_price, unit_cost,
                    discount_percent, gross_amount, net_amount, cost_amount, profit_amount,
                    created_at, order_id, fact_id
                ) VALUES (
                    :onum, :dk, :ck, :ek, :pk,
                    :reg, :stat, :qty, :up, :uc,
                    :disc, :gross, :net, :cost, :profit,
                    :cat_time, :oid, :fid
                )
            """), {
                "onum": order_number,
                "dk": date_key,
                "ck": customer_key,
                "ek": employee_key,
                "pk": product_key,
                "reg": region,
                "stat": status,
                "qty": qty,
                "up": unit_price,
                "uc": unit_cost,
                "disc": discount_pct,
                "gross": gross,
                "net": net,
                "cost": cost,
                "profit": profit,
                "cat_time": dt.strftime("%Y-%m-%d %H:%M:%S"),
                "oid": order_id,
                "fid": fact_id
            })

    logger.info(f"Synchronized order {order_number} (status={status}, net={net}) into fact_sales_order")
    return {"status": "SUCCESS", "orderNumber": order_number, "orderStatus": status}

def sync_all_operational_orders(orders_list: list) -> dict:
    synced = 0
    for order in orders_list:
        try:
            res = sync_operational_order(order)
            if res.get("status") == "SUCCESS":
                synced += 1
        except Exception as e:
            logger.error(f"Error syncing order {order.get('orderNumber')}: {e}")
    return {"status": "SUCCESS", "syncedCount": synced, "totalReceived": len(orders_list)}
