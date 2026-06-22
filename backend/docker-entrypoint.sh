#!/bin/sh
set -e

# Render provides DATABASE_URL as postgres://user:pass@host/db
# Spring needs jdbc:postgresql://user:pass@host/db
if [ -n "$DATABASE_URL" ] && [ -z "$DB_URL" ]; then
  export DB_URL=$(echo "$DATABASE_URL" | sed 's|^postgres://|jdbc:postgresql://|')
fi

# Also handle if DB_URL itself starts with postgres:// (Render blueprint sets it this way)
if [ -n "$DB_URL" ]; then
  export DB_URL=$(echo "$DB_URL" | sed 's|^postgres://|jdbc:postgresql://|' | sed 's|^postgresql://|jdbc:postgresql://|')
fi

exec java -jar /app/app.jar
