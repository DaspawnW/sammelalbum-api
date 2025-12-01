#!/bin/bash

# Container name (adjust if necessary, assuming docker-compose.prod.yml usage)
DB_CONTAINER="sammelalbum-postgres"
TEST_DATA_PATH="../backend/src/main/resources/test-data.sql"

echo "Resetting database..."

# Check if container is running
if [ ! "$(docker ps -q -f name=$DB_CONTAINER)" ]; then
    echo "Error: Container $DB_CONTAINER is not running."
    exit 1
fi

# Copy test data to container
docker cp $TEST_DATA_PATH $DB_CONTAINER:/tmp/test-data.sql

# Execute SQL script
docker exec -i $DB_CONTAINER psql -U user -d sammelalbum -f /tmp/test-data.sql

echo "Database reset complete."
