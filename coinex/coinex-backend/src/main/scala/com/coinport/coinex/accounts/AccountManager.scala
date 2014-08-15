/**
 * Copyright (C) 2014 Coinport Inc. <http://www.coinport.com>
 *
 *
 * MarketManager is the maintainer of a Market. It executes new orders before
 * they are added into a market as pending orders. As execution results, a list
 * of Transactions are generated and returned.
 *
 * MarketManager can be used by an Akka persistent processor or a view
 * to reflect pending orders and market depth.
 *
 * Note this class does NOT depend on event-sourcing framework we choose. Please
 * keep it plain old scala/java.
 */

package com.coinport.coinex.accounts

import org.slf4s.Logging
import scala.collection.mutable.Map

import com.coinport.coinex.data._
import com.coinport.coinex.common._
import Implicits._
import ErrorCode._
import com.coinport.coinex.common.Constants._

class AccountManager(initialLastOrderId: Long = 0L) extends Manager[TAccountState] with Logging {
  // Internal mutable state ----------------------------------------------
  private val accountMap: Map[Long, UserAccount] = Map.empty[Long, UserAccount]
  var aggregationAccount = Map.empty[Currency, CashAccount]
  var lastOrderId = initialLastOrderId
  var lastPaymentId = 0L
  val abCodeMap: Map[Long, ABCodeItem] = Map.empty[Long, ABCodeItem]
  val codeAIndexMap: Map[String, Long] = Map.empty[String, Long]
  val codeBIndexMap: Map[String, Long] = Map.empty[String, Long]

  // Thrift conversions     ----------------------------------------------
  def getSnapshot = TAccountState(
    accountMap.clone,
    getFiltersSnapshot,
    aggregationAccount.clone,
    lastOrderId,
    abCodeMap.clone,
    codeAIndexMap.clone,
    codeBIndexMap.clone,
    Map.empty,
    Map.empty,
    Some(lastPaymentId))

  def loadSnapshot(snapshot: TAccountState) = {
    accountMap.clear
    accountMap ++= snapshot.userAccountsMap
    aggregationAccount.clear
    aggregationAccount ++= snapshot.aggregationAccount
    lastOrderId = snapshot.lastOrderId
    abCodeMap.clear
    codeAIndexMap.clear
    codeBIndexMap.clear
    abCodeMap ++= snapshot.abCodeMap
    codeAIndexMap ++= snapshot.codeAIndexMap
    codeBIndexMap ++= snapshot.codeBIndexMap
    loadFiltersSnapshot(snapshot.filters)
    lastPaymentId = snapshot.lastPaymentId.getOrElse(0)
  }

  // Business logics      ----------------------------------------------
  def transferFundFromLocked(from: Long, to: Long, currency: Currency, amount: Long) = {
    updateCashAccount(from, CashAccount(currency, 0, -amount, 0))
    updateCashAccount(to, CashAccount(currency, amount, 0, 0))
  }

  def transferFundFromAvailable(from: Long, to: Long, currency: Currency, amount: Long) = {
    updateCashAccount(from, CashAccount(currency, -amount, 0, 0))
    updateCashAccount(to, CashAccount(currency, amount, 0, 0))
  }

  def transferFundFromPendingWithdrawal(from: Long, to: Long, currency: Currency, amount: Long) = {
    updateCashAccount(from, CashAccount(currency, 0, 0, -amount))
    updateCashAccount(to, CashAccount(currency, amount, 0, 0))
  }

  def conditionalRefund(condition: Boolean)(currency: Currency, order: Order) = {
    if (condition && order.quantity > 0) refund(order.userId, currency, order.quantity)
  }

  def refund(uid: Long, currency: Currency, quantity: Long) = {
    val current = getUserCashAccount(uid, currency)
    val refundQuantity = scala.math.min(current.locked, quantity)
    if (refundQuantity < quantity) {
      log.warn(s"uid: {$uid} try to refund: {$quantity} in currency: {$currency} which more than locked: {$refundQuantity}")
    }
    updateCashAccount(uid, CashAccount(currency, refundQuantity, -refundQuantity, 0))
  }

  def canUpdateCashAccount(userId: Long, adjustment: CashAccount) = {
    val current = getUserCashAccount(userId, adjustment.currency)
    (current + adjustment).isValid
  }

  def updateCashAccount(accounts: Map[Currency, CashAccount], adjustment: CashAccount) = {
    val current = accounts.getOrElse(adjustment.currency, CashAccount(adjustment.currency, 0, 0, 0))
    val updated = current + adjustment
    assert(updated.isValid)
    accounts += (adjustment.currency -> updated)
  }

  // The coinport account is used to accumulate all fees (including paid and received)
  def updateCoinportAccount(adjustment: CashAccount) = {
    val current = getUserCashAccount(COINPORT_UID, adjustment.currency)
    val updated = current + adjustment
    setUserCashAccount(COINPORT_UID, updated, false)
  }

  def updateCryptoAccount(adjustment: CashAccount) = {
    val current = getUserCashAccount(CRYPTO_UID, adjustment.currency)
    val updated = current + adjustment
    setUserCashAccount(CRYPTO_UID, updated, false)
  }

  def updateCashAccount(userId: Long, adjustment: CashAccount) = {
    assert(userId > 0)
    if (userId == COINPORT_UID) {
      updateCoinportAccount(adjustment)
    } else if (userId == CRYPTO_UID) {
      updateCryptoAccount(adjustment)
    } else {
      val current = getUserCashAccount(userId, adjustment.currency)
      val updated = current + adjustment
      assert(updated.isValid)
      setUserCashAccount(userId, updated)
      val updateAggregation = aggregationAccount.getOrElse(adjustment.currency, CashAccount(adjustment.currency, 0, 0, 0)) + adjustment
      assert(updateAggregation.isValid)
      aggregationAccount += (adjustment.currency -> updateAggregation)
    }
  }

  def getUserAccounts(userId: Long): UserAccount = {
    if (userId == AGGREGATION_UID)
      UserAccount(AGGREGATION_UID, aggregationAccount)
    else
      accountMap.get(userId).getOrElse(UserAccount(userId))
  }

  private def getUserCashAccount(userId: Long, currency: Currency): CashAccount =
    getUserAccounts(userId).cashAccounts.getOrElse(currency, CashAccount(currency, 0, 0, 0))

  private def setUserCashAccount(userId: Long, cashAccount: CashAccount, checkValid: Boolean = true) = {
    if (checkValid && !cashAccount.isValid)
      throw new IllegalArgumentException("Attempted to set user cash account to an invalid value: " + cashAccount)

    val accounts = accountMap.getOrElseUpdate(userId, UserAccount(userId))
    val updated = accounts.copy(cashAccounts = accounts.cashAccounts + (cashAccount.currency -> cashAccount))
    accountMap += userId -> updated
  }

  def getOrderId(): Long = lastOrderId + 1
  def setLastOrderId(id: Long) = { lastOrderId = id }

  def getLastPaymentId(): Long = lastPaymentId + 1
  def setLastPaymentId(id: Long) = { lastPaymentId = id }

  def createABCodeTransaction(userId: Long, codeA: String, codeB: String, amount: Long) {
    val timestamp = System.currentTimeMillis
    val item = ABCodeItem(
      id = abCodeMap.size,
      wUserId = userId,
      codeA = codeA,
      codeB = codeB,
      status = RechargeCodeStatus.Unused,
      amount = amount,
      // default 1 year expiration interval
      created = Some(timestamp),
      updated = Some(timestamp))
    abCodeMap += item.id -> item
    codeAIndexMap += codeA -> item.id
    codeBIndexMap += codeB -> item.id
  }

  def isCodeAAvailable(userId: Long, codeA: String): Boolean = {
    val now = getCurrentTime
    abCodeMap.getOrElse(codeAIndexMap.getOrElse(codeA, -1), None) match {
      case i: ABCodeItem => {
        if (i.status == RechargeCodeStatus.Unused) true
        else if (i.status != RechargeCodeStatus.Frozen) false
        else if (!i.queryExpireTime.isDefined || !i.dUserId.isDefined) false
        else if (now > i.queryExpireTime.get) true
        else if (now <= i.queryExpireTime.get && i.dUserId.get == userId) true
        else false
      }
      case _ => false
    }
  }

  def freezeABCode(userId: Long, codeA: String) {
    abCodeMap += abCodeMap(codeAIndexMap(codeA)).id ->
      abCodeMap(codeAIndexMap(codeA)).copy(dUserId = Some(userId),
        queryExpireTime = Some(getCurrentTime + _1_HOUR),
        status = RechargeCodeStatus.Frozen)
  }

  def isCodeBAvailable(userId: Long, codeB: String): (Boolean, Any) = {
    abCodeMap.getOrElse(codeBIndexMap.getOrElse(codeB, -1), None) match {
      case None => (false, InvalidBCode)
      case i: ABCodeItem => {
        if (i.status.getValue >= 2) (false, UsedBCode)
        else if (i.dUserId.isDefined && userId != i.dUserId.get) (false, InvalidBCode)
        else (true, None)
      }
      case _ => (true, None)
    }
  }

  def verifyConfirm(userId: Long, codeB: String): (Boolean, Any) = {
    println(abCodeMap.getOrElse(codeBIndexMap.getOrElse(codeB, -1), None))
    abCodeMap.getOrElse(codeBIndexMap.getOrElse(codeB, -1), None) match {
      case None => (false, InvalidBCode)
      case i: ABCodeItem => {
        if (i.status.getValue >= 3) (false, UsedBCode)
        else if (i.wUserId == userId) (true, None)
        else (false, InvalidBCode)
      }
      case _ => (false, InvalidBCode)
    }
  }

  def bCodeRecharge(userId: Long, codeB: String) =
    changeRechargeStatus(RechargeCodeStatus.Confirming, userId: Long, codeB: String)

  def confirmRecharge(userId: Long, codeB: String) =
    changeRechargeStatus(RechargeCodeStatus.RechargeDone, userId: Long, codeB: String)

  def changeRechargeStatus(rcStatus: RechargeCodeStatus, userId: Long, codeB: String) {
    abCodeMap += abCodeMap(codeBIndexMap(codeB)).id ->
      abCodeMap(codeBIndexMap(codeB))
      .copy(status = rcStatus, updated = Some(System.currentTimeMillis))
  }

  def getRCDepositRecords(userId: Long): Seq[ABCodeItem] = {
    abCodeMap.values.filter(_.dUserId.getOrElse(-1) == userId).toSeq.sortWith((a, b) => a.id > b.id)
  }

  def getRCWithdrawalRecords(userId: Long): Seq[ABCodeItem] = {
    abCodeMap.values.filter(_.wUserId == userId).toSeq.sortWith((a, b) => a.id > b.id)
  }

  def generateABCode(): (String, String) = {
    val random = new scala.util.Random
    def randStr(source: String)(n: Int): String =
      Stream.continually(random.nextInt(source.size)).map(source).take(n).mkString
    def randABCode(n: Int) =
      randStr("ABCDEF0123456789")(n)
    (randABCode(16), randABCode(32))
  }

  def getCurrentTime(): Long = {
    System.currentTimeMillis / 1000
  }

}
