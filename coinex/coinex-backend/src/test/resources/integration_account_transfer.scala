import com.coinport.coinex.data.Currency
import com.coinport.coinex.transfer.AccountTransferConfig
import com.coinport.coinex.data.Currency._

new AccountTransferConfig {
  override val transferDebug = true
  override val confirmNumMap: collection.Map[Currency, Int] = collection.Map(Btc -> 2, Ltc -> 2, Doge -> 2)
  override val succeededRetainNum: collection.Map[Currency, Int] = collection.Map(Btc -> 100, Ltc -> 200, Doge -> 300)
}