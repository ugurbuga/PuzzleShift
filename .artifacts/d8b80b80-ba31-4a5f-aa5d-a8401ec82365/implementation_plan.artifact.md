# Implementation Plan - Add Fluffy Block Gameplay Identity

Add a new game mode called "Fluffy Block", inspired by "Fluffy Drop". This includes adding a new gameplay style, logic, UI, and wiring it into the existing system.

## User Review Required

> [!IMPORTANT]
> The "Fluffy Drop" mechanic is physics-based. Since the current engine is grid-based, I will implement a grid-based color-matching logic with gravity, which mimics the feel of the original game while staying within the project's architecture.

## Proposed Changes

### Core Models

#### [MODIFY] [GameModels.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/game/model/GameModels.kt)
- Add `FluffyBlock` to `GameplayStyle` enum.
- Update `storageKey()` and `persistedKeys()` for `FluffyBlock`.

### Game Logic

#### [NEW] [FluffyBlockGameLogic.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/game/logic/FluffyBlockGameLogic.kt)
- Implement `GameLogic` for Fluffy Block.
- Core mechanics:
    - Pieces are dropped into columns.
    - Matching colors (adjacent) are cleared.
    - Gravity pulls remaining pieces down.
    - Chain reactions for cascading clears.

#### [MODIFY] [GameLogic.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/game/logic/GameLogic.kt)
- Register `FluffyBlockGameLogic` in `AdaptiveGameLogic`.

### UI

#### [NEW] [FluffyBlockGameScreen.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/ui/game/game/FluffyBlockGameScreen.kt)
- Create the main game screen for Fluffy Block.
- Include Compose Previews.

#### [MODIFY] [GameScreen.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/ui/game/game/GameScreen.kt)
- Register `FluffyBlockGameScreen` in `BlockGamesGameApp` switchboard.

### Onboarding & Resources

#### [NEW] [FluffyBlockOnboardingStateFactory.kt](file:///Users/ugurbuga/Documents/GitHub/StackShift/composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/settings/FluffyBlockOnboardingStateFactory.kt)
- Add interactive onboarding for the new style.

### Android Flavor (Optional but recommended for full identity)

#### [MODIFY] [build.gradle.kts](file:///Users/ugurbuga/Documents/GitHub/StackShift/androidApp/build.gradle.kts)
- Add `fluffyblock` product flavor.

## Verification Plan

### Automated Tests
- Create `FluffyBlockGameLogicTest.kt` to verify color matching and gravity.

### Manual Verification
- Run `jvmRun` and select "Fluffy Block" (once wired through settings/debug menu).
- Verify the game loop: drop, match, clear, gravity, game over.
- Check Compose Previews for `FluffyBlockGameScreen`.
