/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

import com.coinport.coinex.common._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.bitway._
import Constants._
import Implicits._

BitwayConfigs(Map(
  Btc -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    batchFetchAddressNum = 10,
    maintainedChainLength = 10,
    coldAddresses = List("morrnAssmf3LP2UkXST7i3UpKcUbBQX4GU"),
    enableHotColdTransfer = false
  ),
  Ltc -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 20,
    enableHotColdTransfer = false
  ),
  Doge -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 20,
    coldAddresses = List(
      "nUmdT61hFz2MWeHdLbGDjDmdVKVRUiuhnu"),
    enableHotColdTransfer = true
  ),
  Bc -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 20,
    enableHotColdTransfer = false
  ),
  Drk -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 20,
    enableHotColdTransfer = false
  ),
  Vrc -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 120,
    enableHotColdTransfer = false
  ),
  Zet -> BitwayConfig(
    ip = "bitway",
    port = 6379,
    maintainedChainLength = 120,
    enableHotColdTransfer = false
  )
))
