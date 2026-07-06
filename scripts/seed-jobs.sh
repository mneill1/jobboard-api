#!/usr/bin/env bash
# Seeds the running jobboard API with 20 ACTIVE jobs across a variety of cities.
# Requires: curl, jq. Assumes the API is running at $API_BASE (default localhost:8080).
#
# Usage:
#   ./scripts/seed-jobs.sh
#   API_BASE=http://staging.jobboard.local ./scripts/seed-jobs.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
SEED_EMAIL="${SEED_EMAIL:-seed@jobboard.local}"
SEED_PASSWORD="${SEED_PASSWORD:-seedpass123}"
SEED_COMPANY_NAME="${SEED_COMPANY_NAME:-Seed Co}"

command -v jq >/dev/null || { echo "jq is required"; exit 1; }

echo "→ Attempting login as $SEED_EMAIL ..."
login_resp=$(curl -sS -w "\n%{http_code}" -X POST "$API_BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$SEED_EMAIL\",\"password\":\"$SEED_PASSWORD\"}")
login_body=$(echo "$login_resp" | sed '$d')
login_code=$(echo "$login_resp" | tail -n1)

if [ "$login_code" != "200" ]; then
  echo "→ Login failed ($login_code). Registering new COMPANY user ..."
  reg_resp=$(curl -sS -w "\n%{http_code}" -X POST "$API_BASE/api/auth/register" \
      -H 'Content-Type: application/json' \
      -d "$(jq -n \
            --arg email    "$SEED_EMAIL" \
            --arg pass     "$SEED_PASSWORD" \
            --arg cname    "$SEED_COMPANY_NAME" \
            '{email:$email,password:$pass,role:"COMPANY",companyName:$cname,companyIndustry:"Software",companySize:"51-200",companyWebsite:"https://example.com"}')")
  reg_body=$(echo "$reg_resp" | sed '$d')
  reg_code=$(echo "$reg_resp" | tail -n1)
  if [ "$reg_code" != "201" ] && [ "$reg_code" != "200" ]; then
    echo "Register failed with $reg_code: $reg_body"; exit 1
  fi
  TOKEN=$(echo "$reg_body" | jq -r .token)
else
  TOKEN=$(echo "$login_body" | jq -r .token)
fi

[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { echo "No token acquired"; exit 1; }
echo "✓ Auth token acquired"

echo "→ Fetching companyId from /api/users/me ..."
me=$(curl -sS "$API_BASE/api/users/me" -H "Authorization: Bearer $TOKEN")
COMPANY_ID=$(echo "$me" | jq -r .companyId)
[ -n "$COMPANY_ID" ] && [ "$COMPANY_ID" != "null" ] || { echo "No companyId on user: $me"; exit 1; }
echo "✓ companyId=$COMPANY_ID"

# title | location | salaryMin | salaryMax
JOBS=(
  "Senior Backend Engineer|New York, United States|150000|210000"
  "Staff Platform Engineer|New York, United States|180000|240000"
  "Site Reliability Engineer|New York, United States|140000|200000"
  "Data Engineer|New York, United States|130000|185000"
  "Product Designer|London, United Kingdom|75000|105000"
  "Full-Stack Engineer|London, United Kingdom|80000|115000"
  "Engineering Manager|London, United Kingdom|110000|150000"
  "iOS Engineer|Tokyo, Japan|9000000|13000000"
  "ML Research Engineer|Tokyo, Japan|10000000|15000000"
  "Backend Engineer|Berlin, Germany|75000|105000"
  "DevOps Engineer|Berlin, Germany|70000|100000"
  "Frontend Engineer|Singapore, Singapore|110000|150000"
  "Solutions Architect|Singapore, Singapore|140000|190000"
  "Data Scientist|Toronto, Canada|110000|150000"
  "Android Engineer|Sydney, Australia|130000|175000"
  "Growth Engineer|São Paulo, Brazil|180000|260000"
  "QA Automation Engineer|Bangalore, India|1800000|2600000"
  "Backend Engineer|Mumbai, India|1600000|2400000"
  "Product Manager|Cape Town, South Africa|900000|1300000"
  "Security Engineer|Paris, France|75000|110000"
)

DESCRIPTIONS=(
  "We're hiring a strong engineer to work on scalable systems with a small, senior team."
  "Join a fast-moving product org shipping to millions of users daily."
  "You'll partner with product and design to bring polished features from idea to production."
  "Own a critical service end-to-end, from design through operations and observability."
  "Help scale our infrastructure and improve developer experience across the org."
)

echo "→ Creating ${#JOBS[@]} jobs ..."
i=0
for row in "${JOBS[@]}"; do
  i=$((i + 1))
  IFS='|' read -r title location smin smax <<< "$row"
  desc="${DESCRIPTIONS[$((i % ${#DESCRIPTIONS[@]}))]}"

  create_body=$(jq -n \
    --arg title    "$title" \
    --arg desc     "$desc" \
    --arg location "$location" \
    --argjson smin "$smin" \
    --argjson smax "$smax" \
    --argjson cid  "$COMPANY_ID" \
    '{title:$title,description:$desc,location:$location,salaryMin:$smin,salaryMax:$smax,companyId:$cid,status:"DRAFT"}')

  created=$(curl -sS -X POST "$API_BASE/api/jobs" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $TOKEN" \
      -d "$create_body")
  job_id=$(echo "$created" | jq -r .id)
  if [ -z "$job_id" ] || [ "$job_id" = "null" ]; then
    echo "  ✗ [$i] failed to create: $title @ $location — $created"; continue
  fi

  curl -sS -o /dev/null -X PUT "$API_BASE/api/jobs/$job_id/status?status=ACTIVE" \
      -H "Authorization: Bearer $TOKEN"

  printf "  ✓ [%2d] #%s ACTIVE  %-30s  %s\n" "$i" "$job_id" "$title" "$location"
done

echo "✓ Done. Seeded ${#JOBS[@]} active jobs."
