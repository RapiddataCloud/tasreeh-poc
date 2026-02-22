# Tasreeh PO Approval -- Event Driven Microservices POC

## Overview

The Tasreeh PO Approval System is a Proof of Concept (POC) built using a
modern event-driven microservices architecture.

This POC demonstrates:

-   Secure authentication using Keycloak (OAuth2 / OpenID Connect)
-   API Gateway routing using KrakenD
-   Event-driven communication using Kafka (Redpanda)
-   Distributed tracing using OpenTelemetry + Jaeger
-   Spring Boot microservices
-   Angular frontend UI
-   Fully containerized execution using Docker Compose

------------------------------------------------------------------------

# Project Structure

tasreeh-poc/
│
├── Architecture/
├── bpmn/
│
├── frontend/
│   └── tasreeh-po-approval-ui/
│
├── gateway/
│
├── keycloak/
│   └── tasreeh-po-realm.json
│
├── services/
│   ├── order-service/
│   ├── workflow-service/
│   └── notification-service/
│
├── docker-compose.yml
├── otel-collector-config.yml
└── README.md


------------------------------------------------------------------------

# Prerequisites

-   Docker Desktop (latest)
-   Java 17+
-   Maven 3.9+
-   Node 18+
-   Angular CLI

------------------------------------------------------------------------

# Running Entire System (Docker)

From project root:

docker compose down -v 

docker compose up -d --build

This starts:

-   PostgreSQL
-   Redpanda (Kafka)
-   Keycloak
-   KrakenD Gateway
-   OpenTelemetry Collector
-   Jaeger
-   Order Service
-   Workflow Service
-   Notification Service
-   Angular UI

------------------------------------------------------------------------

# Service URLs

Angular UI: http://localhost:4200\
KrakenD Gateway: http://localhost:8080\
Keycloak: http://localhost:8081\
Jaeger UI: http://localhost:16686

------------------------------------------------------------------------

# Database Configuration

jdbc:postgresql://localhost:5432/orderdb\
Username: postgres\
Password: Jan@2026

------------------------------------------------------------------------

# Keycloak Setup

## Access Admin Console

http://localhost:8081

Default Admin Credentials:

Username: admin\
Password: admin

------------------------------------------------------------------------

## Import Realm

1.  Login to Keycloak Admin Console
2.  Click Create Realm
3.  Select Import
4.  Upload file: keycloak/tasreeh-po-realm.json
5.  Click Create

Realm Name: tasreeh-po-realm

------------------------------------------------------------------------

## Create Client (If Not Present)

Clients → Create Client

Client ID: angular-spa\
Client Type: OpenID Connect\
Access Type: Public

Valid Redirect URI: http://localhost:4200/\*

Web Origins: \*

------------------------------------------------------------------------

## Create Test User

Users → Add User

Username: po-user\
Email: po-user@test.com\
Email Verified: ON

Then go to Credentials → Set Password

Password: Password@123\
Temporary: OFF

------------------------------------------------------------------------

# Token Endpoint

http://localhost:8081/realms/tasreeh-po-realm/protocol/openid-connect/token

------------------------------------------------------------------------

# Microservices Ports

Order Service: 8082\
Workflow Service: 8083\
Notification Service: 8084

------------------------------------------------------------------------

# Kafka Configuration

Broker: localhost:9092\
Example Topic: po-created

------------------------------------------------------------------------

# Observability

Jaeger UI:

http://localhost:16686

You can trace full request lifecycle across gateway and services.

------------------------------------------------------------------------

# Stop System

docker compose down

To remove volumes:

docker compose down -v

------------------------------------------------------------------------

# Technology Stack

-   Spring Boot 3.x
-   Angular
-   Redpanda (Kafka compatible)
-   PostgreSQL
-   Keycloak
-   KrakenD
-   OpenTelemetry
-   Jaeger
-   Docker Compose

------------------------------------------------------------------------

# Author

Rapiddata
