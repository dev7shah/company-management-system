import hmac
import logging
from typing import Optional
from fastapi import APIRouter, Depends, Header, HTTPException, Query, Body
from ..config import settings
from ..etl.pipeline import run_etl_pipeline
from ..models.forecast import compute_sales_forecast
from ..models.segmentation import compute_customer_segments
from ..models.sentiment import compute_sentiment_analysis
from ..charts.plotly_builder import (
    build_kpi_metrics,
    build_sales_trend_chart,
    build_regional_comparison_chart,
    build_sales_funnel_chart,
    build_unified_report
)
from ..charts.seaborn_builder import generate_seaborn_plots
from ..etl.live_sync import sync_operational_order, sync_all_operational_orders

logger = logging.getLogger("cms_analytics.endpoints")
router = APIRouter()

def verify_internal_key(x_internal_key: Optional[str] = Header(None)):
    # Internal communication key verification using constant-time comparison
    if settings.INTERNAL_API_KEY:
        if not x_internal_key or not hmac.compare_digest(
            x_internal_key.strip(), settings.INTERNAL_API_KEY.strip()
        ):
            raise HTTPException(status_code=403, detail="Invalid or missing internal API key")
    return True

@router.get("/health")
def health():
    return {"status": "UP", "service": "cms-analytics-engine", "version": "1.0.0"}

@router.post("/etl/run")
def trigger_etl(_=Depends(verify_internal_key)):
    result = run_etl_pipeline()
    return result

@router.get("/etl/status")
def get_etl_status(_=Depends(verify_internal_key)):
    from ..db import engine
    from sqlalchemy import text
    try:
        with engine.connect() as conn:
            row = conn.execute(
                text("SELECT * FROM etl_runs_log ORDER BY run_id DESC LIMIT 1")
            ).mappings().fetchone()
            if row:
                return dict(row)
    except Exception as e:
        logger.warning(f"Error checking ETL status: {e}")
    return {"status": "READY", "message": "No ETL run recorded yet"}

@router.get("/analytics/kpis")
def get_kpis(
    region: Optional[str] = Query(None),
    fromDate: Optional[str] = Query(None),
    toDate: Optional[str] = Query(None),
    _=Depends(verify_internal_key)
):
    return build_kpi_metrics(region, fromDate, toDate)

@router.get("/analytics/sales/trend")
def get_sales_trend(
    region: Optional[str] = Query(None),
    category: Optional[str] = Query(None),
    metric: Optional[str] = Query("all"),
    _=Depends(verify_internal_key)
):
    return build_sales_trend_chart(region, category, metric)

@router.get("/analytics/regional")
def get_regional(
    category: Optional[str] = Query(None),
    _=Depends(verify_internal_key)
):
    return build_regional_comparison_chart(category)

@router.get("/analytics/funnel")
def get_funnel(
    region: Optional[str] = Query(None),
    _=Depends(verify_internal_key)
):
    return build_sales_funnel_chart(region)

@router.get("/analytics/forecast")
def get_forecast(
    horizon: int = Query(6, ge=1, le=24),
    region: Optional[str] = Query(None),
    _=Depends(verify_internal_key)
):
    return compute_sales_forecast(horizon, region)

@router.get("/analytics/segments")
def get_segments(
    k: int = Query(4, ge=2, le=8),
    _=Depends(verify_internal_key)
):
    return compute_customer_segments(k)

@router.get("/analytics/sentiment")
def get_sentiment(
    productId: Optional[int] = Query(None),
    _=Depends(verify_internal_key)
):
    return compute_sentiment_analysis(productId)

@router.get("/analytics/unified-report")
def get_unified_report(
    department: Optional[str] = Query(None),
    _=Depends(verify_internal_key)
):
    return build_unified_report(department)

@router.get("/analytics/advanced-plots")
def get_advanced_plots(_=Depends(verify_internal_key)):
    return generate_seaborn_plots()

@router.post("/analytics/sync-order")
def sync_order(order: dict = Body(...), _=Depends(verify_internal_key)):
    return sync_operational_order(order)

@router.post("/analytics/sync-all-orders")
def sync_all_orders(orders: list = Body(...), _=Depends(verify_internal_key)):
    return sync_all_operational_orders(orders)
