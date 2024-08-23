package electionguard.core

/**
 * Fetches the production-strength [GroupContext] with the desired amount of acceleration via
 * precomputation, which can result in significant extra memory usage.
 *
 * See [PowRadixOption] for the different memory use vs. performance profiles.
 *
 * Also, [ProductionMode] specifies the particular set of cryptographic constants we'll be using.
 */
actual fun productionGroup(
    acceleration: PowRadixOption,
    mode: ProductionMode
): GroupContext {
    TODO("Not yet implemented")
}
