package com.example.geekdiary.data.remote.api

import com.example.geekdiary.data.remote.dto.AssetUploadResponseDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AssetApiService {
    
    /**
     * Download an asset by filename
     * @param filename The filename of the asset to download
     * @return ResponseBody containing the asset data
     */
    @GET("/web/assets/{filename}")
    suspend fun downloadAsset(
        @Path("filename") filename: String
    ): Response<ResponseBody>
    
    /**
     * Upload an asset file
     * @param asset The asset file as multipart body
     * @return AssetUploadResponseDto containing the backend filename
     */
    @Multipart
    @POST("/v1/assets")
    suspend fun uploadAsset(
        @Part asset: MultipartBody.Part
    ): Response<AssetUploadResponseDto>
    
    /**
     * Get asset by path (alternative endpoint from OpenAPI spec)
     * @param path The relative path to the asset file
     * @return ResponseBody containing the asset data
     */
    @GET("/v1/assets")
    suspend fun getAssetByPath(
        @Query("path") path: String
    ): Response<ResponseBody>
}
