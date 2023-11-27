package de.othr.dco.webshop.models

object Entities {
  class User(val userName: String)
  class Item(val itemName: String)

  class Order(val itemList: List[Item], val payment: Payment)
  class Payment(val price: Float)
}
