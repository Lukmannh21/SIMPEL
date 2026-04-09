package com.mbkm.telgo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ModelUtils {

    fun encodeWitel(witel: String?): Double {
        return when (witel?.uppercase()) {
            "ACEH" -> 0.0; "BABEL" -> 1.0; "BENGKULU" -> 2.0; "JAMBI" -> 3.0
            "LAMPUNG" -> 4.0; "MEDAN" -> 5.0; "RIDAR" -> 6.0; "RIKEP" -> 7.0
            "SUMBAR" -> 8.0; "SUMSEL" -> 9.0; "SUMUT" -> 10.0
            else -> 11.0 // UNKNOWN
        }
    }

    fun encodeCatuanAC(catuan: String?): Double {
        return when (catuan?.uppercase()) {
            "EKSISTING BTS" -> 0.0
            "EKSISTING TELKOM" -> 1.0
            "NEW PLN BILLING" -> 2.0
            "NEW PLN TOKEN" -> 3.0
            else -> 4.0 // UNKNOWN
        }
    }

    fun encodeSFP(sfp: String?): Double {
        return when (sfp?.uppercase()) {
            "0" -> 1.0; "10" -> 2.0; "12" -> 3.0; "15" -> 4.0; "16" -> 5.0
            "19" -> 6.0; "20" -> 7.0; "20,7" -> 8.0; "20X2" -> 9.0; "21" -> 10.0
            "23" -> 11.0; "23,5" -> 12.0; "25" -> 13.0; "26" -> 14.0; "30" -> 15.0
            "33" -> 16.0; "35" -> 17.0; "40" -> 18.0; "40X2" -> 19.0; "40X4" -> 20.0
            "48" -> 21.0; "49" -> 22.0; "50" -> 23.0; "52" -> 24.0; "6,3" -> 25.0
            "60" -> 26.0; "70" -> 27.0; "80" -> 28.0; "90" -> 29.0; "93" -> 30.0
            else -> 31.0 // UNKNOWN
        }
    }

    fun encodeKendala(kendala: String?): Double {
        val text = kendala?.lowercase() ?: return 0.0
        val cat1Keywords = listOf("sitac", "permit", "ba survey")
        val cat2Keywords = listOf("waiting redaman", "sfp bidi", "uplink", "migrasi", "new pln", "pondasi", "waiting uplink", "reloc", "upgrade pln", "core node b")

        val hasCat1 = cat1Keywords.any { text.contains(it) }
        val hasCat2 = cat2Keywords.any { text.contains(it) }

        return when {
            hasCat1 && hasCat2 -> 3.0 // Izin & Teknis
            hasCat1 -> 1.0            // Izin dan Dokumen
            hasCat2 -> 2.0            // Masalah Teknis
            else -> 0.0               // Normal/Lainnya
        }
    }

    // --- LOGIKA BARU: PORT METRO BINARY ---
    fun encodePortMetroBinary(port: String?): Double {
        // Jika data kosong atau null = 0.0 (Tidak Ada), jika ada text apapun = 1.0 (Ada Isi)
        return if (port.isNullOrBlank() || port.uppercase() == "NAN") {
            0.0
        } else {
            1.0
        }
    }

    // --- MAPPING STO LENGKAP ---
    fun encodeSto(value: String?): Double {
        return when (value?.uppercase()) {
            "ABA" -> 0.0
            "AGR" -> 1.0
            "ALH" -> 2.0
            "AMB" -> 3.0
            "AMK" -> 4.0
            "ANP" -> 5.0
            "ANR" -> 6.0
            "ARG" -> 7.0
            "ARK" -> 8.0
            "AVI" -> 9.0
            "BAG" -> 10.0
            "BAK" -> 11.0
            "BAS" -> 12.0
            "BBT" -> 13.0
            "BDU" -> 14.0
            "BET" -> 15.0
            "BGO" -> 16.0
            "BGS" -> 17.0
            "BGU" -> 18.0
            "BIJ" -> 19.0
            "BIN" -> 20.0
            "BIR" -> 21.0
            "BJI" -> 22.0
            "BKG" -> 23.0
            "BKJ" -> 24.0
            "BKM" -> 25.0
            "BKN" -> 26.0
            "BKO" -> 27.0
            "BKR" -> 28.0
            "BKT" -> 29.0
            "BLP" -> 30.0
            "BLS" -> 31.0
            "BLT" -> 32.0
            "BNA" -> 33.0
            "BNC" -> 34.0
            "BNN" -> 35.0
            "BPD" -> 36.0
            "BTA" -> 37.0
            "BTC" -> 38.0
            "BTH" -> 39.0
            "BYL" -> 40.0
            "BYU" -> 41.0
            "CAG" -> 42.0
            "CRP" -> 43.0
            "CTD" -> 44.0
            "DAR" -> 45.0
            "DBS" -> 46.0
            "DLM" -> 47.0
            "DLS" -> 48.0
            "DLT" -> 49.0
            "DRI" -> 50.0
            "DUM" -> 51.0
            "GST" -> 52.0
            "HRU" -> 53.0
            "IDA" -> 54.0
            "IDL" -> 55.0
            "IPU" -> 56.0
            "JBS" -> 57.0
            "JHO" -> 58.0
            "JRM" -> 59.0
            "KAG" -> 60.0
            "KBI" -> 61.0
            "KBO" -> 62.0
            "KDS" -> 63.0
            "KDT" -> 64.0
            "KFR" -> 65.0
            "KGG" -> 66.0
            "KGH" -> 67.0
            "KIS" -> 68.0
            "KJI" -> 69.0
            "KLA" -> 70.0
            "KLE" -> 71.0
            "KLK" -> 72.0
            "KMB" -> 73.0
            "KOA" -> 74.0
            "KOB" -> 75.0
            "KOT" -> 76.0
            "KPH" -> 77.0
            "KPI" -> 78.0
            "KRU" -> 79.0
            "KSG" -> 80.0
            "KTL" -> 81.0
            "KTN" -> 82.0
            "KTU" -> 83.0
            "KUL" -> 84.0
            "LBA" -> 85.0
            "LBK" -> 86.0
            "LBP" -> 87.0
            "LBR" -> 88.0
            "LBS" -> 89.0
            "LBY" -> 90.0
            "LGS" -> 91.0
            "LHT" -> 92.0
            "LIM" -> 93.0
            "LIN" -> 94.0
            "LIW" -> 95.0
            "LLG" -> 96.0
            "LOA" -> 97.0
            "LSK" -> 98.0
            "LSM" -> 99.0
            "LTM" -> 100.0
            "LUG" -> 101.0
            "MAB" -> 102.0
            "MBN" -> 103.0
            "MBO" -> 104.0
            "MEM" -> 105.0
            "MET" -> 106.0
            "MGA" -> 107.0
            "MGD" -> 108.0
            "MGR" -> 109.0
            "MIN" -> 110.0
            "MIS" -> 111.0
            "MKO" -> 112.0
            "MNA" -> 113.0
            "MND" -> 114.0
            "MPA" -> 115.0
            "MSK" -> 116.0
            "MTB" -> 117.0
            "MTK" -> 118.0
            "MUA" -> 119.0
            "MUB" -> 120.0
            "MUD" -> 121.0
            "NGB" -> 122.0
            "NTL" -> 123.0
            "OKI" -> 124.0
            "PBD" -> 125.0
            "PBI" -> 126.0
            "PBM" -> 127.0
            "PBR" -> 128.0
            "PDB" -> 129.0
            "PDN" -> 130.0
            "PDP" -> 131.0
            "PDT" -> 132.0
            "PDW" -> 133.0
            "PER" -> 134.0
            "PGA" -> 135.0
            "PGL" -> 136.0
            "PGP" -> 137.0
            "PGW" -> 138.0
            "PKB" -> 139.0
            "PKR" -> 140.0
            "PLG" -> 141.0
            "PLJ" -> 142.0
            "PMB" -> 143.0
            "PMR" -> 144.0
            "PMY" -> 145.0
            "PNA" -> 146.0
            "PNN" -> 147.0
            "PNP" -> 148.0
            "PPN" -> 149.0
            "PPT" -> 150.0
            "PRA" -> 151.0
            "PRP" -> 152.0
            "PRR" -> 153.0
            "PRT" -> 154.0
            "PRW" -> 155.0
            "PSB" -> 156.0
            "PSP" -> 157.0
            "PUB" -> 158.0
            "PWG" -> 159.0
            "PYB" -> 160.0
            "PYK" -> 161.0
            "RAP" -> 162.0
            "RBB" -> 163.0
            "RGT" -> 164.0
            "SAB" -> 165.0
            "SAK" -> 166.0
            "SBG" -> 167.0
            "SBL" -> 168.0
            "SBN" -> 169.0
            "SCC" -> 170.0
            "SGB" -> 171.0
            "SGD" -> 172.0
            "SGI" -> 173.0
            "SGP" -> 174.0
            "SGT" -> 175.0
            "SIB" -> 176.0
            "SIE" -> 177.0
            "SIH" -> 178.0
            "SIP" -> 179.0
            "SIR" -> 180.0
            "SJJ" -> 181.0
            "SKL" -> 182.0
            "SKY" -> 183.0
            "SLA" -> 184.0
            "SLG" -> 185.0
            "SLJ" -> 186.0
            "SLK" -> 187.0
            "SLN" -> 188.0
            "SLT" -> 189.0
            "SMM" -> 190.0
            "SNA" -> 191.0
            "SOK" -> 192.0
            "SPE" -> 193.0
            "SPN" -> 194.0
            "SPP" -> 195.0
            "SPS" -> 196.0
            "SPU" -> 197.0
            "SRJ" -> 198.0
            "SRW" -> 199.0
            "SSI" -> 200.0
            "STB" -> 201.0
            "STNM" -> 202.0
            "SUM" -> 203.0
            "SUN" -> 204.0
            "SUR" -> 205.0
            "SWL" -> 206.0
            "SYA" -> 207.0
            "TAK" -> 208.0
            "TBH" -> 209.0
            "TBI" -> 210.0
            "TBK" -> 211.0
            "TGD" -> 212.0
            "TIB" -> 213.0
            "TIS" -> 214.0
            "TJK" -> 215.0
            "TJM" -> 216.0
            "TJN" -> 217.0
            "TJP" -> 218.0
            "TJR" -> 219.0
            "TJU" -> 220.0
            "TKN" -> 221.0
            "TLB" -> 222.0
            "TLN" -> 223.0
            "TLU" -> 224.0
            "TMB" -> 225.0
            "TMO" -> 226.0
            "TMU" -> 227.0
            "TPI" -> 228.0
            "TPJ" -> 229.0
            "TPN" -> 230.0
            "TRA" -> 231.0
            "TRS" -> 232.0
            "TSS" -> 233.0
            "TTG" -> 234.0
            "TTJ" -> 235.0
            "TTN" -> 236.0
            "TUB" -> 237.0
            "TUK" -> 238.0
            "TWI" -> 239.0
            "UBT" -> 240.0
            "UGD" -> 241.0
            "UNT" -> 243.0
            "WJA" -> 244.0
            else -> 242.0 // UNKNOWN/DEFAULT
        }
    }

    fun encodeWeekPlan(value: String?): Double {
        return when (value?.uppercase()) {
            "APR W1" -> 0.0
            "APR W2" -> 1.0
            "APR W3" -> 2.0
            "APR W4" -> 3.0
            "APR W5" -> 4.0
            "AUG W1" -> 5.0
            "AUG W2" -> 6.0
            "AUG W3" -> 7.0
            "AUG W4" -> 8.0
            "DEC W1" -> 9.0
            "DEC W2" -> 10.0
            "DEC W3" -> 11.0
            "DEC W4" -> 12.0
            "FEB W1" -> 13.0
            "FEB W2" -> 14.0
            "FEB W3" -> 15.0
            "FEB W4" -> 16.0
            "FEB W5" -> 17.0
            "JAN W0" -> 18.0
            "JAN W1" -> 19.0
            "JAN W2" -> 20.0
            "JAN W3" -> 21.0
            "JAN W5" -> 22.0
            "JUL W1" -> 23.0
            "JUL W2" -> 24.0
            "JUL W3" -> 25.0
            "JUL W4" -> 26.0
            "JUL W5" -> 27.0
            "JUN W1" -> 28.0
            "JUN W2" -> 29.0
            "JUN W3" -> 30.0
            "JUN W4" -> 31.0
            "JUN W5" -> 32.0
            "MAR W1" -> 33.0
            "MAR W2" -> 34.0
            "MAR W3" -> 35.0
            "MAR W4" -> 36.0
            "MAR W5" -> 37.0
            "MAY W1" -> 38.0
            "MAY W2" -> 39.0
            "MAY W3" -> 40.0
            "MAY W4" -> 41.0
            "MAY W5" -> 42.0
            "NOV W1" -> 43.0
            "NOV W2" -> 44.0
            "NOV W3" -> 45.0
            "NOV W4" -> 46.0
            "NOV W5" -> 47.0
            "OCT W1" -> 48.0
            "OCT W2" -> 49.0
            "OCT W3" -> 50.0
            "OCT W4" -> 51.0
            "OCT W5" -> 52.0
            "SEP W1" -> 53.0
            "SEP W2" -> 54.0
            "SEP W3" -> 55.0
            "SEP W4" -> 56.0
            "SEP W5" -> 57.0
            else -> 58.0 // UNKNOWN/DEFAULT (Sesuai JSON)
        }
    }

    // --- MAPPING SiteProvider ---
    fun encodeSiteProvider(value: String?): Double {
        return when (value?.uppercase()) {
            "#NAME?" -> 0.0
            "CAMAT" -> 1.0
            "CITRALAND" -> 2.0
            "DMT" -> 3.0
            "DMT - BIFURCATION" -> 4.0
            "DMT - RESELLER" -> 5.0
            "IBS" -> 6.0
            "KADES" -> 7.0
            "NO NEED SITAC" -> 8.0
            "NOT READY" -> 9.0
            "PROTELINDO" -> 10.0
            "PT CENTRATAMA MENARA INDONESIA" -> 11.0
            "PT GAMETRACO TUNGGAL" -> 12.0
            "PT GIHON TELEKOMUNIKASI INDONESIA" -> 13.0
            "PT KOMET INFRA NUSANTARA" -> 14.0
            "PT QUATTRO INTERNATIONAL" -> 15.0
            "PT. ERA BANGUN TOWERINDO" -> 16.0
            "PT. PROTELINDO" -> 17.0
            "PEMKO" -> 18.0
            "READY" -> 19.0
            "SDN 3" -> 20.0
            "STO ROOM" -> 21.0
            "STP" -> 22.0
            "TBG" -> 23.0
            "TELKOM" -> 24.0 // Merujuk ke index 24, 26, dan 33 (duplicate di dataset asli)
            "TSEL" -> 25.0
            "TELKOMSEL" -> 27.0
            "TOWER BERSAMA GROUP" -> 28.0
            "WARGA" -> 30.0 // Merujuk ke index 30 dan 31 (duplicate di dataset asli)
            "YAYASAN HIMMATUL UMMAH" -> 32.0
            else -> 29.0 // UNKNOWN/DEFAULT (Sesuai JSON)
        }
    }

    // --- HITUNG UMUR ---
    fun calculateUmur(startProjectDate: String?): Double {
        if (startProjectDate.isNullOrEmpty()) return 0.0

        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MMM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )

        var date: Date? = null
        for (format in formats) {
            try {
                date = format.parse(startProjectDate)
                if (date != null) break
            } catch (e: Exception) { continue }
        }

        return if (date != null) {
            val diff = Date().time - date.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toDouble()
        } else {
            0.0
        }
    }
}