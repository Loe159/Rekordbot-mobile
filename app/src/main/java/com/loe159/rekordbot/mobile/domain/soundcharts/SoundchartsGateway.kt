package com.loe159.rekordbot.mobile.domain.soundcharts

interface SoundchartsGateway {
    suspend fun fetchTrackMetadata(
        spotifyTrackId: String,
        configuration: SoundchartsConfiguration,
    ): Result<SoundchartsMetadata>
}
