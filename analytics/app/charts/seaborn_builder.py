import io
import base64
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns
from ..db import engine

def generate_seaborn_plots():
    query_sales = """
    SELECT 
        f.quantity,
        f.unit_price,
        f.unit_cost,
        f.discount_percent,
        f.net_amount,
        f.profit_amount,
        p.category,
        f.region
    FROM fact_sales_order f
    JOIN dim_product p ON f.product_key = p.product_key
    WHERE f.order_status IN ('CONFIRMED', 'PAID', 'DELIVERED')
    """
    query_feedback = """
    SELECT rating, sentiment_score, sentiment_label
    FROM fact_feedback
    """

    with engine.connect() as conn:
        df = pd.read_sql(query_sales, conn)
        df_fb = pd.read_sql(query_feedback, conn)
        
    if df.empty:
        return {
            "correlationHeatmap": "",
            "categoryBoxPlot": "",
            "regionalViolinPlot": "",
            "sentimentRatingPlot": ""
        }
        
    # 1. Correlation Heatmap
    plt.figure(figsize=(7, 5))
    numeric_df = df[["quantity", "unit_price", "discount_percent", "net_amount", "profit_amount"]].rename(
        columns={
            "quantity": "Qty",
            "unit_price": "Unit Price",
            "discount_percent": "Discount %",
            "net_amount": "Net Rev",
            "profit_amount": "Profit"
        }
    )
    corr = numeric_df.corr()
    sns.heatmap(corr, annot=True, cmap="coolwarm", fmt=".2f", linewidths=0.5, cbar=True)
    plt.title("Financial Metrics Correlation Matrix", fontsize=12, pad=12)
    plt.tight_layout()
    
    buf_corr = io.BytesIO()
    plt.savefig(buf_corr, format="png", dpi=120)
    plt.close()
    buf_corr.seek(0)
    b64_corr = base64.b64encode(buf_corr.read()).decode("utf-8")
    
    # 2. Boxplot: Profit Distribution by Product Category
    plt.figure(figsize=(8, 5))
    sns.boxplot(x="category", y="profit_amount", data=df, palette="Set2", showmeans=True)
    plt.title("Net Profit Spread by Product Category", fontsize=12, pad=12)
    plt.xlabel("Category")
    plt.ylabel("Profit Amount (₹ INR)")
    plt.xticks(rotation=15)
    plt.tight_layout()
    
    buf_box = io.BytesIO()
    plt.savefig(buf_box, format="png", dpi=120)
    plt.close()
    buf_box.seek(0)
    b64_box = base64.b64encode(buf_box.read()).decode("utf-8")

    # 3. Violin Plot: Regional Margin / Revenue Distribution
    plt.figure(figsize=(8, 5))
    sns.violinplot(x="region", y="net_amount", data=df, palette="Pastel1", inner="quartile")
    plt.title("Geographic Deal Size & Density Distribution", fontsize=12, pad=12)
    plt.xlabel("Market Region")
    plt.ylabel("Net Order Amount (₹ INR)")
    plt.tight_layout()

    buf_violin = io.BytesIO()
    plt.savefig(buf_violin, format="png", dpi=120)
    plt.close()
    buf_violin.seek(0)
    b64_violin = base64.b64encode(buf_violin.read()).decode("utf-8")

    # 4. Rating vs Sentiment Score Boxplot
    if not df_fb.empty:
        plt.figure(figsize=(8, 5))
        sns.boxplot(x="rating", y="sentiment_score", data=df_fb, palette="Blues_r", showmeans=True)
        plt.title("Customer Rating (1-5★) vs NLP Sentiment Polarity", fontsize=12, pad=12)
        plt.xlabel("Review Rating (Stars)")
        plt.ylabel("TextBlob Polarity Score (-1 to +1)")
        plt.axhline(0, color="gray", linestyle="--", linewidth=0.8, alpha=0.7)
        plt.tight_layout()

        buf_sent = io.BytesIO()
        plt.savefig(buf_sent, format="png", dpi=120)
        plt.close()
        buf_sent.seek(0)
        b64_sent = base64.b64encode(buf_sent.read()).decode("utf-8")
    else:
        b64_sent = ""
    
    return {
        "correlationHeatmap": f"data:image/png;base64,{b64_corr}",
        "categoryBoxPlot": f"data:image/png;base64,{b64_box}",
        "regionalViolinPlot": f"data:image/png;base64,{b64_violin}",
        "sentimentRatingPlot": f"data:image/png;base64,{b64_sent}" if b64_sent else ""
    }
