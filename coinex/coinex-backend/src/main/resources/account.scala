/**
 * Copyright 2014 Coinport Inc. All Rights Reserved.
 * Author: c@coinport.com (Chao Ma)
 */

import com.coinport.coinex.accounts._
import com.coinport.coinex.api.model._
import com.coinport.coinex.common._
import com.coinport.coinex.data._
import com.coinport.coinex.data.Currency._
import com.coinport.coinex.fee._
import Constants._
import Implicits._

AccountConfig(
  feeConfig = FeeConfig(
    marketFeeRules = Map(
      (Btc ~> Ltc) -> PercentageFee(0.001),
      (Ltc ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Doge) -> PercentageFee(0.001),
      (Doge ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Pts) -> PercentageFee(0.001),
      (Pts ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Drk) -> PercentageFee(0.001),
      (Drk ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Bc) -> PercentageFee(0.001),
      (Bc ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Vrc) -> PercentageFee(0.001),
      (Vrc ~> Btc) -> PercentageFee(0.001),

      (Btc ~> Zet) -> PercentageFee(0.001),
      (Zet ~> Btc) -> PercentageFee(0.001)
    ),

    robotFeeRules = Map(
      TRAILING_STOP_ORDER_ROBOT_TYPE -> PercentageFee(0.003),
      STOP_ORDER_ROBOT_TYPE -> PercentageFee(0.002)),

    transferFeeRules = Map(
      Btc -> ConstantFee(0.0002.internalValue(Btc)),
      Ltc -> ConstantFee(0.0002.internalValue(Ltc)),
      Doge -> ConstantFee(0.0002.internalValue(Doge)),
      Pts -> ConstantFee(0.0002.internalValue(Pts)),
      Drk -> ConstantFee(0.0002.internalValue(Drk)),
      Bc -> ConstantFee(0.0002.internalValue(Bc)),
      Vrc -> ConstantFee(0.0002.internalValue(Vrc)),
      Zet -> ConstantFee(0.0002.internalValue(Zet)),
      Cny -> PercentageFee(0.005)),

    freeOfTxChargeUserIdThreshold = 1E9.toLong + 2000 // 1 thousand
    )
)
