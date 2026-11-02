import pandas as pd
from collections import Counter
import re
from ..db import engine
from sqlalchemy import text

STOPWORDS = {
    "the", "and", "a", "to", "of", "in", "is", "for", "with", "our", "we", "this",
    "that", "it", "on", "as", "at", "by", "an", "be", "was", "are", "from", "or",
    "have", "has", "had", "not", "but", "very", "all", "so", "if", "too", "been",
    "their", "they", "will", "would", "could", "should", "during", "after", "before"
}

ASPECT_PATTERNS = {
    "Performance & Speed": re.compile(r"\b(speed|performance|fast|timeout|slow|load|latency|throughput|uptime|scale)\b", re.I),
    "Security & Compliance": re.compile(r"\b(security|compliance|audit|zero-trust|protection|shield|safe|confidential)\b", re.I),
    "Customer Support & SLA": re.compile(r"\b(support|service|ticket|resolution|team|manager|account|sla|help|assist)\b", re.I),
    "Integration & APIs": re.compile(r"\b(integration|api|documentation|migration|connector|cloud|setup|pipeline|deploy)\b", re.I),
    "Pricing & ROI Value": re.compile(r"\b(price|pricing|cost|roi|licensing|value|expensive|billing|investment)\b", re.I)
}

def compute_sentiment_analysis(product_id: int = None):
    query = """
    SELECT 
        fb.feedback_key,
        fb.rating,
        fb.review_text,
        fb.sentiment_label,
        fb.sentiment_score,
        COALESCE(p.name, 'All Solutions') as product_name,
        COALESCE(p.category, 'General') as product_category,
        d.year,
        d.month,
        d.month_name
    FROM fact_feedback fb
    JOIN dim_date d ON fb.date_key = d.date_key
    LEFT JOIN dim_product p ON fb.product_key = p.product_key
    """
    params = {}
    if product_id:
        query += " WHERE fb.product_key = :product_id"
        params["product_id"] = int(product_id)
        
    with engine.connect() as conn:
        df = pd.read_sql(text(query), conn, params=params)
        
    if df.empty:
        return {
            "totalReviews": 0,
            "averageRating": 0.0,
            "netPromoterScore": 0.0,
            "sentimentDistribution": {"positive": 0, "neutral": 0, "negative": 0},
            "topKeywords": [],
            "ratingVsSentiment": [],
            "monthlyTrend": [],
            "aspectBreakdown": [],
            "productRankings": []
        }
        
    total_reviews = len(df)
    avg_rating = round(float(df["rating"].mean()), 2)
    
    # Net Promoter Score (NPS) proxy
    promoters = int((df["rating"] == 5).sum())
    passives = int((df["rating"] == 4).sum())
    detractors = int((df["rating"] <= 3).sum())
    nps_score = round(((promoters - detractors) / total_reviews) * 100, 1)

    # Sentiment Counts
    sentiment_counts = df["sentiment_label"].value_counts().to_dict()
    pos = int(sentiment_counts.get("POSITIVE", 0))
    neu = int(sentiment_counts.get("NEUTRAL", 0))
    neg = int(sentiment_counts.get("NEGATIVE", 0))
    
    # Top Keywords Extraction
    words = []
    aspect_scores = {k: [] for k in ASPECT_PATTERNS}

    for _, row in df.iterrows():
        text_val = str(row["review_text"])
        score = float(row["sentiment_score"])
        
        # Aspects
        for aspect_name, pattern in ASPECT_PATTERNS.items():
            if pattern.search(text_val):
                aspect_scores[aspect_name].append(score)

        cleaned = re.sub(r"[^\w\s]", "", text_val.lower())
        tokens = [w for w in cleaned.split() if len(w) > 3 and w not in STOPWORDS]
        words.extend(tokens)
        
    counter = Counter(words)
    top_keywords = [{"word": w, "count": c} for w, c in counter.most_common(16)]

    # Aspect Sentiment Breakdown
    aspect_breakdown = []
    for aspect_name, scores in aspect_scores.items():
        if scores:
            aspect_breakdown.append({
                "aspect": aspect_name,
                "mentions": len(scores),
                "avgSentiment": round(float(sum(scores) / len(scores)), 3),
                "positivePct": round((sum(1 for s in scores if s > 0.1) / len(scores)) * 100, 1)
            })
    aspect_breakdown.sort(key=lambda x: x["mentions"], reverse=True)
    
    # Rating vs Sentiment
    rating_sentiment = []
    for r in range(1, 6):
        sub = df[df["rating"] == r]
        rating_sentiment.append({
            "rating": r,
            "count": len(sub),
            "pctOfTotal": round((len(sub) / total_reviews) * 100, 1),
            "avgSentiment": round(float(sub["sentiment_score"].mean()), 3) if not sub.empty else 0.0
        })
        
    # Monthly Sentiment Trend
    df["year_month"] = df["year"].astype(str) + "-" + df["month"].astype(str).str.zfill(2)
    monthly = df.groupby("year_month").agg({
        "sentiment_score": "mean",
        "rating": "mean",
        "feedback_key": "count"
    }).reset_index().rename(columns={"feedback_key": "reviews_count"}).sort_values(by="year_month")
    
    monthly_trend = []
    for _, row in monthly.iterrows():
        monthly_trend.append({
            "month": str(row["year_month"]),
            "avgSentiment": round(float(row["sentiment_score"]), 3),
            "avgRating": round(float(row["rating"]), 2),
            "count": int(row["reviews_count"])
        })

    # Product-level CSAT rankings
    prod_grp = df.groupby(["product_name", "product_category"]).agg({
        "rating": ["mean", "count"],
        "sentiment_score": "mean"
    }).reset_index()
    prod_grp.columns = ["product_name", "category", "avg_rating", "reviews_count", "avg_sentiment"]
    prod_grp = prod_grp.sort_values(by="avg_rating", ascending=False)

    product_rankings = []
    for _, row in prod_grp.head(10).iterrows():
        product_rankings.append({
            "productName": str(row["product_name"]),
            "category": str(row["category"]),
            "avgRating": round(float(row["avg_rating"]), 2),
            "reviewsCount": int(row["reviews_count"]),
            "avgSentiment": round(float(row["avg_sentiment"]), 3)
        })
        
    return {
        "totalReviews": total_reviews,
        "averageRating": avg_rating,
        "netPromoterScore": nps_score,
        "promotersCount": promoters,
        "passivesCount": passives,
        "detractorsCount": detractors,
        "sentimentDistribution": {
            "positive": pos,
            "neutral": neu,
            "negative": neg,
            "positivePct": round(pos / total_reviews * 100, 1),
            "neutralPct": round(neu / total_reviews * 100, 1),
            "negativePct": round(neg / total_reviews * 100, 1)
        },
        "topKeywords": top_keywords,
        "ratingVsSentiment": rating_sentiment,
        "aspectBreakdown": aspect_breakdown,
        "productRankings": product_rankings,
        "monthlyTrend": monthly_trend
    }
