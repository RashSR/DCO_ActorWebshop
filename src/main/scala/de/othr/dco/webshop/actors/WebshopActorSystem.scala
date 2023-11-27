package de.othr.dco.webshop.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import de.othr.dco.webshop.actors.BasketActor.AllItemsForUser
import de.othr.dco.webshop.models.Entities.{Item, User}

object WebshopActorSystem {
  sealed trait UserCommandMessage
  case class AddItemToBasket(user: User, item: Item) extends UserCommandMessage
  case class MakeOrder(user: User) extends UserCommandMessage
  case class AdaptedBasketResponse(response: BasketActor.BasketResponse) extends UserCommandMessage

  def apply(): Behavior[UserCommandMessage] = Behaviors.setup(
    setupCxt => {
      //Spawns needed actors
      val mapperRef: ActorRef[BasketActor.BasketResponse] = setupCxt.messageAdapter(response => AdaptedBasketResponse(response))

      val basketActor: ActorRef[BasketActor.BasketRequest] = setupCxt.spawn(BasketActor(), "basket-actor")
      handleUserMessage(basketActor, mapperRef)
    }
  )


  private def handleUserMessage(basketActor: ActorRef[BasketActor.BasketRequest], mapperRef: ActorRef[BasketActor.BasketResponse]): Behavior[UserCommandMessage] =
    Behaviors.receiveMessage(
      msg => msg match {
        //This is request handling for user input
        case AddItemToBasket(user, item) => {
          println(s"[WebshopActorSystem] User: ${user.userName} --- Item: ${item.itemName}")
          basketActor ! BasketActor.AddItemToUserBasket(user, item, mapperRef)
          Behaviors.same
        }
        case MakeOrder(user) =>{
          println(s"[WebshopActorSystem] ${user.userName} wants to make an order.")
          basketActor ! BasketActor.GetAllItemForUser(user, mapperRef)
          Behaviors.same
        }
        //--------------------------------------

        //This is response handling for adapter responses
        case AdaptedBasketResponse(basketResponse) => {
          //This is only needed due to param basketResponse being of type BasketActor.BasketResponse
          //So another case match is needed for all implementations of BasketActor.BasketResponse
          //In this simple example only one case is implemented
          basketResponse match {
            case AllItemsForUser(itemList) =>{
              println("[WebshopActorSystem] print received Basket ")
              itemList.foreach(x => println(x.itemName))
              println("[WebshopActorSystem] --------------------")
              Behaviors.same
            }
          }
        }
        //-------------------
      }
    )

}
