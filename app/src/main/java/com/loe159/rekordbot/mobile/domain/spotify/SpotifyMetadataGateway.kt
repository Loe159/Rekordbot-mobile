package com.loe159.rekordbot.mobile.domain.spotify

interface SpotifyMetadataGateway {
    suspend fun fetchTrackMetadata(trackId: String): Result<SpotifyTrackMetadata>
}
