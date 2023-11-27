package de.othr.dco.webshop.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import de.othr.dco.webshop.models.Entities.{Item, Payment}

object PaymentActor {

  sealed trait PaymentRequest
  case class CollectPayment(payment: Payment, replyTo: ActorRef[PaymentCollected]) extends PaymentRequest
  case class PaymentCollected(status: String)

  def apply(): Behaviors.Receive[PaymentRequest] = {
    Behaviors.receiveMessage[PaymentRequest] {
      case CollectPayment(payment, replyTo) => {
        println(s"[PaymentActor] try to make a payment for ${payment.price} USD.")
        val rand = new scala.util.Random
        //Simulate workload
        val randWaitTime = rand.between(0, 10)
        Thread.sleep(randWaitTime * 1000)
        //-----
        if (rand.between(0, 2) == 0)
        {
          replyTo ! PaymentCollected("OK")
        }
        else {
          replyTo ! PaymentCollected("NOK")
        }

        Behaviors.same
        }

    }
  }
}
