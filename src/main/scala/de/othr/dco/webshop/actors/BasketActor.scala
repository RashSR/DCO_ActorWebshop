package de.othr.dco.webshop.actors

import akka.actor.typed.{ActorRef, Behavior, scaladsl}
import akka.actor.typed.scaladsl.Behaviors
import de.othr.dco.webshop.actors.WebshopActorSystem.UserCommandMessage
import de.othr.dco.webshop.models.Entities.{Item, User}

import scala.::

object BasketActor {

  sealed trait BasketRequest
  sealed trait BasketResponse

  case class AddItemToUserBasket(user: User, item: Item, ref:ActorRef[AllItemsForUser]) extends BasketRequest
  case class GetAllItemForUser(user: User, ref:ActorRef[AllItemsForUser]) extends BasketRequest
  case class AllItemsForUser(itemList: List[Item]) extends BasketResponse

  var database = collection.mutable.Map[User, List[Item]]()

  //See: WebshopActorSystem for different function-based implementation of receiveMessage
  def apply(): Behavior[BasketRequest] = Behaviors.setup(
    setupCxt => {
      Behaviors.receiveMessage(
        msg => msg match {
          case AddItemToUserBasket(user, item, ref) =>
            println(s"[BasketActor] Trying to add item ${item.itemName} to basket from user: ${user.userName}")

            if(!database.contains(user)){
              //Create new user sub-database
              database = database + (user -> List[Item]())
            }
            database(user) = item :: database(user)

            ref ! AllItemsForUser(database(user))
            Behaviors.same

          case GetAllItemForUser(user, ref) =>

            println(s"[BasketActor] Trying to get all Items for user: ${user.userName}")
            if(!database.contains(user))
            {
              ref ! AllItemsForUser(List())
            }
            else
            {
              ref ! AllItemsForUser(database(user))
            }

            Behaviors.same
        }
      )
    }
  )
}
