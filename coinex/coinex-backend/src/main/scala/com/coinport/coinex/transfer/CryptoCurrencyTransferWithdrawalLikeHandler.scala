package com.coinport.coinex.transfer

import com.coinport.coinex.data._
import com.coinport.coinex.data.TransferStatus.Confirming

class CryptoCurrencyTransferWithdrawalLikeHandler extends CryptoCurrencyTransferHandler {
  def this(item: CryptoCurrencyTransferItem)(implicit env: TransferEnv) {
    this()
    this.item = item
    setEnv(env, None)
  }

  def this(t: AccountTransfer, from: Option[CryptoCurrencyTransactionPort], to: Option[CryptoCurrencyTransactionPort], timestamp: Option[Long])(implicit env: TransferEnv) {
    this()
    setEnv(env, timestamp)
    // id, currency, sigId, txid, userId, from, to(user's internal address), includedBlock, txType, status, userToHotMapedDepositId, accountTransferId, created, updated
    item = CryptoCurrencyTransferItem(env.manager.getNewTransferItemId, t.currency, None, None, Some(t.userId), from, to, None, Some(t.`type`), Some(Confirming), None, Some(t.id), timestamp)
    saveItemToMongo()
  }
}
