#!/bin/bash

set -e
set -u

function create_user_and_database() {
    local database=$1
    echo "  Creating user and database '$database'"

    # 1. Créer l'utilisateur et la base en tant que superutilisateur, l'utilisateur étant propriétaire de la base
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE USER $database WITH PASSWORD '$POSTGRES_PASSWORD';
        CREATE DATABASE $database OWNER $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $database;
EOSQL

    # 2. Corriger les permissions du schéma pour PostgreSQL 15+
    # Se connecter directement à la base créée et attribuer la propriété du schéma public à l'utilisateur de la base
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$database" <<-EOSQL
        ALTER SCHEMA public OWNER TO $database;
        GRANT ALL ON SCHEMA public TO $database;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_user_and_database $db
    done
    echo "Multiple databases created"
fi
