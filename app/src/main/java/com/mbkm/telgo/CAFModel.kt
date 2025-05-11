package com.mbkm.telgo

import java.io.Serializable

data class CAFModel(
    val id: String = "",
    val todayDate: String = "",
    val coloApplicationDate: String = "",
    val revision: String = "",


    // Site Information
    val siteId: String = "",
    val island: String = "Sumatera", // Default value
    val province: String = "",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val siteType: String = "",
    val towerType: String = "",
    val buildingHeight: String = "",
    val towerHeight: String = "",
    val towerExtensionRequired: Boolean = false,

    // Client Information
    val client: String = "",
    val clientSiteId: String = "",
    val clientSiteName: String = "",
    val clientContact: String = "",
    val contactPhone: String = "",
    val siteAddress: String = "",

    // Equipment Details
    val btsAntennas: List<AntennaItem> = listOf(),
    val mwAntennas: List<AntennaItem> = listOf(),
    val amplifiers: List<AmplifierItem> = listOf(),

    // Shelter/Power Requirements
    val equipmentType: String = "", // INDOOR, OUTDOOR, OTHER
    val equipmentPadLength: String = "",
    val equipmentPadWidth: String = "",
    val electricityKVA: String = "",
    val electricityPhases: String = "",
    val permanentGensetRequired: Boolean = false,
    val gensetLength: String = "",
    val gensetWidth: String = "",
    val remarks: String = "",

    // Signatures
    val accountManagerSignature: String = "",
    val accountManagerDate: String = "",
    val qualityControlSignature: String = "",
    val qualityControlDate: String = "",
    val colocationSignature: String = "",
    val colocationDate: String = "",
    val clientSignature: String = "",
    val clientDate: String = "",

    // Metadata
    val createdBy: String = "",
    val createdAt: String = "",
    val excelUrl: String = "",
    val drawingUrl: String = "",
    var accountManagerName: String = "",

    var qualityControlName: String = "",

    var colocationName: String = "",

    var clientName: String = ""
) : Serializable

// Update the existing data classes in CAFModel.kt
data class AntennaItem(
    val itemNo: String = "",
    var status: String = "",
    var height: String = "",
    var quantity: String = "",
    var manufacturer: String = "",
    var model: String = "",
    var dimensions: String = "",
    var azimuth: String = "",
    var cableQuantity: String = "",
    var cableSize: String = "",
    var remarks: String = ""
) : Serializable

data class AmplifierItem(
    val itemNo: String = "",
    var status: String = "",
    var height: String = "",
    var quantity: String = "",
    var manufacturer: String = "",
    var model: String = "",
    var dimensions: String = "",
    var azimuth: String = "",
    var cableQuantity: String = "",
    var cableSize: String = "",
    var remarks: String = ""
) : Serializable

