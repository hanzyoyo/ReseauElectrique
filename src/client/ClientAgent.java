package client;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ClientAgent extends Agent{
	private int meanConsumption;
	private int varianceConsumption = 5;

	private int meanProduction = 0;
	private int varianceProduction = 0;

	private double monthlyTotal = 0; // what if producer asks before first tick?

	class Producer{
		private AID name = null;
		private int prix;

		public AID getName() {
			return name;
		}
		public void setName(AID name) {
			this.name = name;
		}
		public int getPrix() {
			return prix;
		}
		public void setPrix(int prix) {
			this.prix = prix;
		}
	}

	private Producer producer = new Producer();

	public void setup(){
		// Get the mean consumption/production of electricity in the arguments
		Object[] args = getArguments();

		if(args != null &&	args.length > 0) {
			meanConsumption	= Integer.parseInt((String) args[0]);
			if (args.length > 1){
				meanProduction = Integer.parseInt((String) args[1]);
			}
			monthlyTotal = meanConsumption - meanProduction;
		}

		//add one-shot behavior to subscribe to Producer
		addBehaviour(new SubscriptionBehaviour());

		//add ticked behavior to simulate random consumption every month
		addBehaviour(new TickerBehaviour(this,5000) {

			@Override
			protected void onTick() {
				// TODO Auto-generated method stub
				Random rndm = new Random();
				double newConsumption = rndm.nextGaussian() * varianceConsumption + meanConsumption;
				double newProduction = rndm.nextGaussian() * varianceProduction + meanProduction;
				monthlyTotal = newConsumption - newProduction;
			}
		});

		//add cyclic behavior to handle requests for monthly consumption
		addBehaviour(new CyclicBehaviour(this) {

			@Override
			public void action() {

				ClientAgent myClient = (ClientAgent)myAgent;
				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchSender(myClient.producer.getName()), MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
				ACLMessage msg = myAgent.receive(mt);

				if(msg != null){
					//log
					System.out.println("Client "+myClient.getLocalName()+" a reçu une demande de consommation");

					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.INFORM);
					reply.setContent(String.valueOf(monthlyTotal));
					reply.setConversationId("conso");
					myAgent.send(reply);				
				}
				else{
					block();
				}
			}
		});


	}

	class SubscriptionBehaviour extends Behaviour{ //problem if producer is not registered yet since it's oneshot + we do not go through all steps since it's oneshot
		private int step = 0;

		@Override
		public void action() {

			MessageTemplate mt = null;

			// consult DFService and take first (for now) Producer
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("electricity-producer");
			template.addServices(sd);

			try{
				DFAgentDescription[] results = DFService.search(myAgent, template);

				//need to test if at least one producer is registered
				if(results.length != 0){
					switch(step){
					case 0:
						//request price to all electricity producers
						ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
						for(int i = 0; i < results.length; i++){
							cfp.addReceiver(results[i].getName());
						}
						cfp.setConversationId("Demande Prix");
						cfp.setReplyWith("cfp"+System.currentTimeMillis());
						myAgent.send(cfp);
						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Demande Prix"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

						//log
						System.out.println("Agent " + myAgent.getLocalName() + " envoie CFP");

						step=1;
						break;
					case 1:
						ACLMessage reply = myAgent.receive(mt);
						if(reply != null){						
							if(reply.getPerformative() == ACLMessage.PROPOSE){ //what if message received is an information but not on the prices?
								//log
								System.out.println("Client "+ myAgent.getLocalName() + " reçoit proposition du Producteur " + reply.getSender().getLocalName());

								//keep producer with cheapest price
								//content of message split in two parts : prixVente/prixAchat
								String[] prixVenteAchat = reply.getContent().split("/");
								int prixVente = Integer.parseInt(prixVenteAchat[0]);
								int prixAchat = Integer.parseInt(prixVenteAchat[1]);								
								
								ClientAgent myClient = (ClientAgent)myAgent;
								
								//computation of mensual price
								int price = prixVente*myClient.meanConsumption - prixAchat*myClient.meanProduction;
								
								//log
								System.out.println(reply.getSender().getLocalName() + " propose un prix moyen mensuel de " + price + " à " + myAgent.getLocalName());

								if(myClient.producer.getName() == null || price < myClient.producer.getPrix()){
									//if first answer received create timeout. what if no answer received?
									if(myClient.producer.getName() == null){
										myAgent.addBehaviour(new WakerBehaviour(myAgent,1000){
											protected void handleElapsedTimeout() {

												//log
												System.out.println("Timeout Elapsed");

												SubscriptionBehaviour.this.step=2;
											}
										});
									}
									//update local variable
									myClient.producer.setName(reply.getSender());
									myClient.producer.setPrix(price);								
								}
							}
						}else
							block();
						break;
					case 2:
						//TODO : don't forget to send refusal to all other producers or they are stuck
						//send proposal agreement and start subscription
						ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						msg.addReceiver(((ClientAgent)myAgent).producer.getName());
						msg.setConversationId("Subscription");
						msg.setReplyWith("Subscription"+System.currentTimeMillis());
						myAgent.send(msg);

						//log
						System.out.println("Agent " + myAgent.getLocalName() + " envoie demande d'abonnement au Producteur " + ((ClientAgent)myAgent).producer.getName().getLocalName());

						mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Subscription"),MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
						step=3;
						break;
					case 3:
						//wait for subscription acknowledgment
						reply = myAgent.receive(mt);
						if(reply != null){
							if(reply.getPerformative() == ACLMessage.INFORM){
								//log
								System.out.println("Client "+myAgent.getLocalName()+" est abonné au Producteur "+reply.getSender().getLocalName());
								step=4;
							}
						}
						else{
							block();
						}
						break;
					}
				}
			}catch(FIPAException e){
				e.printStackTrace();
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return step==4;
		}
	}
}

