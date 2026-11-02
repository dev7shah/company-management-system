import os
import csv
import random
from datetime import datetime, timedelta

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "analytics", "data")
os.makedirs(DATA_DIR, exist_ok=True)

REGIONS = ["North", "South", "East", "West", "Central"]

CATEGORIES = {
    "Enterprise Software": [
        ("PRD-CRM-01", "Cloud CRM Enterprise", 1200.00, 350.00),
        ("PRD-ERP-02", "Smart ERP Suite", 2500.00, 700.00),
        ("PRD-HCM-03", "HCM Workforce Pro", 1100.00, 320.00),
        ("PRD-SCM-04", "Supply Chain Optimizer", 1850.00, 520.00)
    ],
    "Cloud Services & Infra": [
        ("PRD-CLD-05", "Virtual Cluster 16C", 450.00, 120.00),
        ("PRD-STR-06", "HighIO Storage 10TB", 300.00, 80.00),
        ("PRD-NET-07", "Secure CDN Pipeline", 600.00, 150.00),
        ("PRD-K8S-08", "Kubernetes Mesh Engine", 950.00, 260.00),
        ("PRD-GWY-09", "Multi-Cloud API Gateway", 750.00, 210.00)
    ],
    "AI & Data Intelligence": [
        ("PRD-AI-10", "Predictive Analytics Engine", 2200.00, 600.00),
        ("PRD-GEN-11", "GenAI Copilot Workspace", 3200.00, 850.00),
        ("PRD-FRD-12", "Real-Time Fraud Detection", 2800.00, 780.00),
        ("PRD-DW-13", "Star-Schema Warehouse Appliance", 3500.00, 950.00),
        ("PRD-AML-14", "Automated ML Pipeline", 1950.00, 510.00)
    ],
    "Cyber Defense & Security": [
        ("PRD-SEC-15", "CyberShield Pro", 800.00, 200.00),
        ("PRD-ZT-16", "Zero-Trust Identity Shield", 1650.00, 420.00),
        ("PRD-THR-17", "Threat Intelligence Sentinel", 1400.00, 380.00),
        ("PRD-CMP-18", "Cloud Compliance Auditor", 1250.00, 310.00)
    ],
    "Consulting & Support": [
        ("PRD-CON-19", "Architecture Advisory", 1500.00, 400.00),
        ("PRD-SUP-20", "24/7 Dedicated Support", 950.00, 250.00),
        ("PRD-OPS-21", "DevSecOps Transformation", 2100.00, 550.00)
    ]
}

EMPLOYEES = [
    ("EMP-001", "Sarah Jenkins", "Sales", "Senior Account Executive"),
    ("EMP-002", "David Chen", "Sales", "Enterprise Solutions Specialist"),
    ("EMP-003", "Elena Rostova", "Sales", "Regional Sales Director"),
    ("EMP-004", "Marcus Vance", "Sales", "Client Success Manager"),
    ("EMP-005", "Amina Patel", "Sales", "Key Account Lead"),
    ("EMP-006", "Lucas Wright", "Sales", "Strategic Accounts VP"),
    ("EMP-007", "Kavita Reddy", "Sales", "Commercial Growth Executive"),
    ("EMP-008", "Jonathan Blake", "Sales", "Global Cloud Solutions Lead"),
    ("EMP-009", "Mei-Ling Zhou", "Sales", "AI & Analytics Specialist"),
    ("EMP-010", "Carlos Santana", "Sales", "Enterprise Cyber Defense Advisor"),
    ("EMP-011", "Zara Al-Mansoor", "Sales", "FinTech Solutions Director"),
    ("EMP-012", "Liam O'Connor", "Sales", "Infrastructure Accounts Executive")
]

CUSTOMERS = [
    (101, "Acme Corporation", "North", "Alice Smith", "alice@acme.com"),
    (102, "Nexus Global Tech", "West", "Bob Martin", "bmartin@nexusglobal.com"),
    (103, "Apex Retail Systems", "East", "Clara Oswald", "coswald@apexretail.com"),
    (104, "Horizon Logistics", "South", "Daniel Craig", "dan@horizonlog.com"),
    (105, "BlueSky Finance", "Central", "Evelyn Reed", "evelyn@blueskyfin.com"),
    (106, "Titan Industrial", "North", "Frank Castle", "frank@titanind.com"),
    (107, "Summit Energy Group", "West", "Grace Hopper", "grace@summitenergy.com"),
    (108, "Vanguard Healthcare", "East", "Henry McCoy", "hmccoy@vanguardhealth.com"),
    (109, "Pinnacle Media Group", "South", "Isabella Ross", "iross@pinnacle.com"),
    (110, "Zenith Global Telecom", "Central", "Jack Reacher", "jreacher@zenith.com"),
    (111, "Quantum Dynamics", "North", "Karen Page", "kpage@quantum.io"),
    (112, "OmniCorp International", "West", "Leo Fitz", "lfitz@omnicorp.org"),
    (113, "Solaria Clean Tech", "East", "Mia Wong", "mia@solaria.com"),
    (114, "Atlas Manufacturing", "South", "Nathan Drake", "ndrake@atlasmfg.com"),
    (115, "Nova Financial Partners", "Central", "Olivia Pope", "opope@novafin.com"),
    (116, "AeroTech Propulsion Labs", "North", "Patrick Stewart", "pstewart@aerotech.com"),
    (117, "Beacon Health Systems", "East", "Quinn Fabray", "qfabray@beaconhealth.org"),
    (118, "Cascadia Cloud Services", "West", "Rachel Green", "rgreen@cascadiacloud.io"),
    (119, "Delta Freight & Maritime", "South", "Samuel Jackson", "sjackson@deltafreight.com"),
    (120, "Equinox Media Holdings", "Central", "Tara Maclay", "tmaclay@equinoxmedia.com"),
    (121, "Frontier Robotics Corp", "North", "Ulysses Grant", "ugrant@frontierbot.com"),
    (122, "Genesis BioInformatics", "West", "Victor Stone", "vstone@genesisbio.org"),
    (123, "Hyperion Power Grid", "East", "Wanda Maximoff", "wanda@hyperionpower.com"),
    (124, "Infinity Insurance Group", "Central", "Xavier Woods", "xwoods@infinityins.com"),
    (125, "Jupiter Defense Systems", "South", "Yolanda Adams", "yadams@jupiterdefense.gov"),
    (126, "Kinetic Automotive", "North", "Zachary Levi", "zlevi@kineticauto.com"),
    (127, "Lumina Semiconductor", "West", "Althea Vance", "avance@luminasemi.com"),
    (128, "Metropolis Banking Hub", "East", "Bruce Wayne", "bwayne@metropolisbank.com"),
    (129, "Nordic Supply Chain", "North", "Christine Palmer", "cpalmer@nordicsupply.eu"),
    (130, "Orion Satellite Comms", "West", "Diana Prince", "dprince@orionsat.com"),
    (131, "Paragon Pharma Labs", "Central", "Edward Nygma", "enygma@paragonpharma.com"),
    (132, "Qualia Cognitive AI", "East", "Felicity Smoak", "fsmoak@qualiaai.io"),
    (133, "Radiant Solar Energy", "South", "Gareth Bale", "gbale@radiantsolar.com"),
    (134, "Starlight Retailers Inc", "Central", "Hal Jordan", "hjordan@starlightretail.com"),
    (135, "Triton Marine Shipping", "South", "Iris West", "iwest@tritonmarine.com"),
    (136, "Ultima Aerospace Group", "North", "John Stewart", "jstewart@ultimaaero.com"),
    (137, "Veritas Legal Tech", "East", "Kate Bishop", "kbishop@veritaslaw.com"),
    (138, "Wavelength Telecom", "West", "Lois Lane", "llane@wavelength.com"),
    (139, "Xenon Advanced Materials", "Central", "Matt Murdock", "mmurdock@xenonmat.com"),
    (140, "Yosemite Digital Agency", "West", "Natasha Romanoff", "nromanoff@yosemitedigital.com"),
    (141, "Zephyr Microgrid Tech", "South", "Oliver Queen", "oqueen@zephyrmicro.com"),
    (142, "Apex BioTherapeutics", "East", "Peter Parker", "pparker@apexbio.org"),
    (143, "Bravura Entertainment", "West", "Quentin Lance", "qlance@bravuraent.com"),
    (144, "Crestview Capital Trust", "Central", "Barry Allen", "ballen@crestviewtrust.com"),
    (145, "Dynasty Luxury Brands", "North", "Selina Kyle", "skyle@dynastybrands.com"),
    (146, "Elysium Cyber Defense", "South", "Tony Stark", "tstark@elysiumcyber.io"),
    (147, "Fortress Data Solutions", "East", "Arthur Curry", "acurry@fortressdata.com"),
    (148, "Guardian Life Sciences", "North", "Barbara Gordon", "bgordon@guardianlife.org"),
    (149, "Helios Hydrogen Systems", "West", "Clark Kent", "ckent@heliosh2.com"),
    (150, "Ironclad Infrastructure", "Central", "Dinah Lance", "dlance@ironcladinfra.com"),
    (151, "Krypton Cloud Network", "North", "Evan Daniels", "edaniels@kryptoncloud.net"),
    (152, "Leviathan Deep Sea Tech", "South", "Francine Frye", "ffrye@leviathantech.com"),
    (153, "Mirage Virtual Worlds", "West", "Gwen Stacy", "gstacy@miragevr.io"),
    (154, "Nimbus Weather Analytics", "Central", "Hank Pym", "hpym@nimbusweather.com"),
    (155, "Omega Precision Optics", "East", "Janet Van Dyne", "jvandyne@omegaoptics.com"),
    (156, "Pulse Health Monitoring", "South", "Miles Morales", "mmorales@pulsehealth.io"),
    (157, "Quasar Quantum Computing", "North", "Otto Octavius", "ooctavius@quasarquantum.com"),
    (158, "Raptor Defense Dynamics", "South", "Reed Richards", "rrichards@raptordefense.com"),
    (159, "Sovereign Trust Bank", "East", "Susan Storm", "sstorm@sovereigntrust.com"),
    (160, "Tesseract Space Systems", "West", "Victor Von Doom", "vdoom@tesseractspace.org")
]

REVIEW_TEMPLATES = [
    # 5 Stars - High Praise
    (5, "Exceptional enterprise platform performance and top-notch customer support. Streamlined our entire supply chain workflow!"),
    (5, "Very satisfied with the speed and reliability. High return on investment within the very first operating quarter."),
    (5, "Unmatched security compliance and outstanding scalability under heavy production enterprise loads."),
    (5, "The AI predictive analytics and data warehouse models gave our executive board invaluable decision clarity."),
    (5, "Fantastic uptime and seamless multi-cloud deployment. Our DevOps team experienced zero downtime during migration."),
    (5, "The customer success manager went above and beyond during integration. Best enterprise SaaS vendor we have worked with."),
    (5, "Intuitive user interface coupled with powerhouse analytical horsepower. Our sales reps adopted it immediately."),
    (5, "Game changer for our financial compliance and automated reporting workflows. Highly recommend to any enterprise."),
    (5, "Superb API stability, thorough documentation, and lightning fast query responses even across millions of records."),
    
    # 4 Stars - Strong with Minor Suggestions
    (4, "Great enterprise software with rich features. Initial onboarding had minor learning curve but smooth sailing afterwards."),
    (4, "Reliable infrastructure and responsive account managers. Would definitely recommend to industry peers."),
    (4, "Cost-effective licensing and seamless billing process. Highly pleased with the overall service quality and responsiveness."),
    (4, "Very solid performance and dependable analytics. Would appreciate a few more third-party pre-built connectors."),
    (4, "Robust role-based security and detailed audit logs. Documentation on advanced configurations could be slightly expanded."),
    (4, "Fast customer support turnaround and dependable SLA. Reporting charts look crisp and professional in board meetings."),
    (4, "Scales smoothly across our regional offices. A minor hiccup in timezone conversion was patched rapidly by tech support."),
    
    # 3 Stars - Neutral / Mixed Feedback
    (3, "Decent overall features, but reporting dashboard could use more customization options and custom formula fields."),
    (3, "Meets core expectations, though pricing is somewhat on the higher end for small departmental pilot deployments."),
    (3, "Adequate feature set for day-to-day operations. Performance is acceptable during standard hours but slows slightly on month-end."),
    (3, "Standard capabilities matching market alternatives. Onboarding tutorials could be modernized with interactive videos."),
    (3, "Solid core features, though mobile responsiveness and tablet dashboard viewports need design polish."),
    (3, "The analytics are powerful, but exporting large historical datasets took longer than anticipated."),

    # 2 Stars - Constructive Criticism
    (2, "Experienced periodic integration delays during initial cloud migration. Technical documentation needs comprehensive overhaul."),
    (2, "Frequent timeout errors during month-end batch processing and somewhat slow ticket resolution from Level 1 tech support."),
    (2, "Licensing cost increases feel steep given the pace of new feature releases over the past two quarters."),
    (2, "Encountered sync discrepancies between operational sales orders and data warehouse reports. Required manual re-indexing."),

    # 1 Star - Unhappy / Escalations
    (1, "Critical delay in API gateway response during Black Friday sales traffic. Escalated to executive leadership."),
    (1, "Support tickets took over 72 hours for critical priority issues. Expected higher responsiveness for tier-1 enterprise pricing.")
]

def generate_historical_sales(num_records=4500):
    filepath = os.path.join(DATA_DIR, "historical_sales.csv")
    # Span 3.5 years (from mid-2023 to late 2026)
    start_date = datetime(2023, 4, 1)
    end_date = datetime(2026, 10, 25)
    total_days = (end_date - start_date).days
    
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow([
            "order_number", "order_date", "customer_id", "customer_name", "region",
            "employee_code", "employee_name", "product_sku", "product_name", "category",
            "quantity", "unit_price", "unit_cost", "discount_pct", "gross_amount", "net_amount",
            "cost_amount", "profit_amount", "status"
        ])
        
        all_products = []
        for cat, prods in CATEGORIES.items():
            for sku, name, price, cost in prods:
                all_products.append((sku, name, cat, price, cost))
        
        statuses = ["DELIVERED", "PAID", "CONFIRMED", "QUOTED", "INQUIRY", "CANCELLED"]
        status_weights = [0.58, 0.22, 0.10, 0.05, 0.03, 0.02]

        random.seed(42)  # Reproducible high-quality distribution
        
        for i in range(1, num_records + 1):
            # Introduce realistic seasonality & yearly expansion
            day_offset = random.randint(0, total_days)
            cur_date = start_date + timedelta(days=day_offset)
            year = cur_date.year
            month = cur_date.month

            # Realistic growth multiplier: 2023 base (1.0x) -> 2024 (1.18x) -> 2025 (1.38x) -> 2026 (1.60x)
            growth_weight = 1.0 + (year - 2023) * 0.18
            # Q4 surge (October-December budget flush)
            if month in (10, 11, 12):
                growth_weight *= 1.35
            elif month in (3, 6, 9): # Quarter-end closes
                growth_weight *= 1.15
                
            order_date_str = cur_date.strftime("%Y-%m-%d")
            order_num = f"ORD-{year}-{10000 + i}"
            
            cust = random.choice(CUSTOMERS)
            emp = random.choice(EMPLOYEES)
            prod = random.choice(all_products)
            
            # Quantity with business realism (occasional large enterprise bulk orders)
            if random.random() < 0.12:
                qty = random.randint(15, 45) # Large enterprise deployment
            else:
                qty = random.randint(1, 14)
                
            unit_price = prod[3]
            unit_cost = prod[4]
            discount_pct = random.choices([0.0, 0.05, 0.10, 0.15, 0.20], weights=[0.40, 0.25, 0.20, 0.10, 0.05])[0]
            
            gross = round(qty * unit_price, 2)
            net = round(gross * (1.0 - discount_pct), 2)
            cost = round(qty * unit_cost, 2)
            profit = round(net - cost, 2)
            
            status = random.choices(statuses, weights=status_weights)[0]
            
            writer.writerow([
                order_num, order_date_str, cust[0], cust[1], cust[2],
                emp[0], emp[1], prod[0], prod[1], prod[2],
                qty, unit_price, unit_cost, discount_pct * 100, gross, net,
                cost, profit, status
            ])
            
    print(f"Generated {num_records} enhanced historical sales rows -> {filepath}")

def generate_reviews(num_records=1200):
    filepath = os.path.join(DATA_DIR, "reviews.csv")
    start_date = datetime(2023, 4, 1)
    end_date = datetime(2026, 10, 25)
    total_days = (end_date - start_date).days
    
    all_skus = []
    for cat, prods in CATEGORIES.items():
        for sku, _, _, _ in prods:
            all_skus.append(sku)
            
    random.seed(42)

    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["review_id", "customer_id", "product_sku", "rating", "review_text", "created_date"])
        
        for i in range(1, num_records + 1):
            rating, text = random.choice(REVIEW_TEMPLATES)
            
            # Subtle variation / jitter
            if rating == 5:
                prefix = random.choice(["Verified Buyer: ", "Five-star experience: ", "Enterprise deployment: ", "Highly recommended: ", ""])
            elif rating == 4:
                prefix = random.choice(["Great outcome: ", "Very solid tool: ", "Good performance: ", ""])
            elif rating == 3:
                prefix = random.choice(["Neutral feedback: ", "Standard results: ", "Mixed review: ", ""])
            else:
                prefix = random.choice(["Critical feedback: ", "Attention required: ", "Service issue: ", ""])
                
            cust = random.choice(CUSTOMERS)
            sku = random.choice(all_skus)
            day_offset = random.randint(0, total_days)
            review_date = (start_date + timedelta(days=day_offset)).strftime("%Y-%m-%d")
            
            writer.writerow([
                f"REV-{5000 + i}", cust[0], sku, rating, prefix + text, review_date
            ])
            
    print(f"Generated {num_records} enhanced customer reviews rows -> {filepath}")

if __name__ == "__main__":
    generate_historical_sales(4500)
    generate_reviews(1200)
