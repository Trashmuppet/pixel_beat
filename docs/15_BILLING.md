# Billing

## Business Model
One-time Google Play Pro Unlock.

Price: £1.99

## Platform
Google Play Billing Library
Target SDK: Android 16 (API 36)

## Architecture

feature modules
      ↓
PremiumManager
      ↓
billing module
      ↓
Google Play Billing

## Rules
- No subscriptions.
- No advertisements.
- No accounts.
- No telemetry.
- Offline entitlement cache after purchase.
- Core beat creation remains free.

## Pro Features
- Advanced exports
- Additional scene packs
- Premium cosmetic content

Project files never depend on entitlement state.
