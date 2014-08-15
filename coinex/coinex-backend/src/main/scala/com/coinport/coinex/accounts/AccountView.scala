/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 */

package com.coinport.coinex.accounts

import akka.event.{ LoggingAdapter, LoggingReceive }
import akka.persistence.Persistent
import com.coinport.coinex.common.ExtendedView
import com.coinport.coinex.data._
import Implicits._
import com.coinport.coinex.fee.CountFeeSupport
import com.coinport.coinex.common.PersistentId._
import com.coinport.coinex.common.Constants._
import com.coinport.coinex.fee.FeeConfig

class AccountView(accountConfig: AccountConfig) extends ExtendedView with AccountManagerBehavior {
  val feeConfig = accountConfig.feeConfig
  override val processorId = ACCOUNT_PROCESSOR <<
  override val viewId = ACCOUNT_VIEW<<
  val manager = new AccountManager(0L)
  implicit val logger: LoggingAdapter = null

  def receive = LoggingReceive {
    case Persistent(msg, _) => updateState(msg)

    case QueryAccount(userId) => sender ! QueryAccountResult(manager.getUserAccounts(userId))

    case QueryRCDepositRecord(userId) => sender ! QueryRCDepositRecordResult(manager.getRCDepositRecords(userId))

    case QueryRCWithdrawalRecord(userId) => sender ! QueryRCWithdrawalRecordResult(manager.getRCWithdrawalRecords(userId))

    case QueryFeeConfig => sender ! QueryFeeConfigResult(feeConfig.toThrift)
  }
}
