#!/bin/bash
echo "========================================"
echo "  GiftGPT Dev Setup (No Docker Required)"
echo "========================================"
echo ""

echo "[1/3] Building Java backend..."
export JAVA_HOME="$HOME/jdk-17/jdk-17.0.19+10"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && mvn clean install -DskipTests -q && cd ..
echo ""

echo "[2/3] Installing Python dependencies..."
cd backend/giftgpt-ai && pip install -r requirements.txt -q && cd ../..
echo ""

echo "[3/3] Installing frontend dependencies..."
cd frontend/giftgpt-web && npm install && cd ../..
echo ""

echo "========================================"
echo "  Setup complete!"
echo ""
echo "  Start commands (run each in separate terminal):"
echo ""
echo "  1. Java API:"
echo "     cd backend/giftgpt-server && mvn spring-boot:run"
echo ""
echo "  2. Python AI:"
echo "     cd backend/giftgpt-ai && uvicorn main:app --port 8000 --reload"
echo ""
echo "  3. Frontend:"
echo "     cd frontend/giftgpt-web && npm run dev"
echo ""
echo "  Access URLs:"
echo "  - Frontend:    http://localhost:3000"
echo "  - Java API:    http://localhost:8080"
echo "  - Swagger:     http://localhost:8080/swagger-ui.html"
echo "  - H2 Console:  http://localhost:8080/h2-console"
echo "  - Python AI:   http://localhost:8000/docs"
echo "========================================"
