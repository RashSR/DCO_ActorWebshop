package de.othr.dco.webshop

import akka.actor.typed.{ActorRef, ActorSystem}
import de.othr.dco.webshop.actors.WebshopActorSystem
import de.othr.dco.webshop.actors.WebshopActorSystem.{AddItemToBasket, MakeOrder, UserCommandMessage}
import de.othr.dco.webshop.models.Entities.{Item, User}

object Webshop extends App {
  val system: ActorRef[UserCommandMessage] = ActorSystem(WebshopActorSystem(), "my-best-system")
  val testUser = new User("TestUser")
  system ! AddItemToBasket(testUser, new Item("Rose"))
  system ! AddItemToBasket(testUser, new Item("Gitarre"))
  system ! MakeOrder(testUser)
}
