#!/bin/bash
set -e

COMPANY_ID=$(curl -s -X POST http://localhost:8080/api/companies \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp"}' | jq -r '.id')

echo "Company $COMPANY_ID created; seeding 500 jobs..."

for i in {1..500}; do
    curl -s -f -X POST http://localhost:8080/api/jobs \
      -H "Content-Type: application/json" \
      -d "{\"title\":\"Job $i\",\"description\":\"Description $i\",\"companyId\":$COMPANY_ID,\"location\":\"Remote\",\"salaryMin\":50000,\"salaryMax\":100000}" \
      > /dev/null || echo "failed on job $i"
done
echo "Done"
