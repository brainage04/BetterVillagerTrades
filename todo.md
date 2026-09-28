# BetterVillagerTrades todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [ ] Low: the Edit Game Rules screen shows raw translation keys for both gamerules; the jar has no lang file.
- [ ] Low: the README gives the gamerule ids without the `bettervillagertrades:` namespace.
- [ ] Low: nitwits can be rerolled and get the success message (`TradeRerollService.java:97` only rejects profession `NONE`).
