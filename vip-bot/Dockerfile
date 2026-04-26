FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV VIP_API_HOST=0.0.0.0
ENV VIP_API_PORT=8787

CMD ["python", "bot.py"]
