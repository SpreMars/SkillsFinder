#!/bin/bash

echo "=== Building SkillsFinder ==="

# Build backend
echo "Building backend..."
cd backend
docker build -t skillsfinder-backend:latest .
cd ..

# Build frontend
echo "Building frontend..."
cd frontend
docker build -t skillsfinder-frontend:latest ..
cd ..

# Start services
echo "Starting services..."
docker-compose up -d

echo "=== Done ==="
echo "Frontend: http://localhost"
echo "Backend API: http://localhost:8080"
