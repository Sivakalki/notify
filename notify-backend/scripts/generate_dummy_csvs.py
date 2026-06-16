import csv
import random
import string
import os

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "dummy_data")
os.makedirs(OUTPUT_DIR, exist_ok=True)

DOMAINS = ["gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "proton.me", "icloud.com"]
FIRST_NAMES = ["james", "emma", "liam", "olivia", "noah", "ava", "william", "sophia",
               "oliver", "isabella", "elijah", "mia", "lucas", "amelia", "mason", "harper",
               "logan", "evelyn", "ethan", "abigail", "aiden", "ella", "jackson", "scarlett"]
LAST_NAMES  = ["smith", "johnson", "williams", "brown", "jones", "garcia", "miller", "davis",
               "wilson", "anderson", "taylor", "thomas", "hernandez", "moore", "martin",
               "jackson", "lee", "perez", "thompson", "white", "harris", "clark", "lewis"]

FILES = [
    {"name": "cohort_premium_users.csv",    "count": 10000},
    {"name": "cohort_trial_users.csv",      "count": 8500},
    {"name": "cohort_inactive_users.csv",   "count": 9200},
    {"name": "cohort_newsletter_users.csv", "count": 7800},
    {"name": "cohort_beta_testers.csv",     "count": 6300},
]

def random_suffix(n=6):
    return "".join(random.choices(string.digits, k=n))

def random_phone():
    return f"+1{random.randint(2000000000, 9999999999)}"

def generate_row(index, file_index):
    first  = random.choice(FIRST_NAMES)
    last   = random.choice(LAST_NAMES)
    suffix = random_suffix()
    uid    = f"user_{file_index}_{index:05d}_{suffix}"
    email  = f"{first}.{last}{suffix}@{random.choice(DOMAINS)}"
    # ~10% of users have no email, ~15% have no phone, but at least one must exist
    drop_email = random.random() < 0.10
    drop_phone = random.random() < 0.15
    if drop_email and drop_phone:
        drop_email = False  # guarantee at least email is present
    email = "" if drop_email else email
    phone = "" if drop_phone else random_phone()
    return [uid, email, phone]

for fi, spec in enumerate(FILES, start=1):
    path = os.path.join(OUTPUT_DIR, spec["name"])
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["externalUserId", "email", "phone"])
        for i in range(1, spec["count"] + 1):
            writer.writerow(generate_row(i, fi))
    print(f"Generated {spec['name']}  ({spec['count']:,} rows)")

print(f"\nAll files saved to: {OUTPUT_DIR}")