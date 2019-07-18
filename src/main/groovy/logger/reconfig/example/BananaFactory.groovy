package logger.reconfig.example

/**
 * Factory that gets bananas.
 */
interface BananaFactory {

    /**
     * Gets a banana.
     * @param origin Country of origin to get banana from.
     * @return A banana from specified origin. Or banana
     * from somewhere else if cannot serve request for specified origin.
     */
    Banana getBananaFrom(String origin)

}
