# Build Sequence

## Rule

Build infrastructure before features.

## Order

1. Repository
2. Core models
3. Project format (.mbeat)
4. Timeline compiler
5. Offline renderer
6. Audio engine
7. Scene runtime
8. Sequencer UI
9. Arrangement UI
10. Export pipeline
11. Billing
12. Optimisation

## Mandatory Gates

- Tests passing
- Performance budgets met
- Deterministic output verified
- Offline behaviour verified
- Documentation updated
- ADR created for architectural changes

Never skip milestones.
Never redesign frozen architecture during implementation.
