package client;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
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
	private int varianceConsumption;

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
			meanConsumption	= (int) args[0];
			if (args.length > 1){
				meanProduction = (int) args[1];
			}
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
				ACLMessage msg = myAgent.receive();

				if(msg != null){
					//if received message is a request from the agent's producer send monthly consumption
					if(msg.getSender() == ((ClientAgent)myAgent).producer.getName() && msg.getPerformative() == ACLMessage.REQUEST){
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(String.valueOf(monthlyTotal));
						myAgent.send(reply);
					}					
				}
				else{
					block();
				}
			}
		});


	}

	class SubscriptionBehaviour extends OneShotBehaviour{
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
				//Producer = result[0].getName();

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

					++step;
					break;
				case 1:
					ACLMessage reply = myAgent.receive(mt);
					if(reply != null){
						if(reply.getPerformative() == ACLMessage.PROPOSE){ //what if message received is an information but not on the prices?
							//keep producer with cheapest price
							int price = Integer.parseInt(reply.getContent());
							ClientAgent myClient = (ClientAgent)myAgent;

							if(myClient.producer.getName() == null || price < myClient.producer.getPrix()){
								myClient.producer.setName(reply.getSender());
								myClient.producer.setPrix(price);

								//if first answer received create timeout. what if no answer received?
								if(myClient.producer.getName() == null){
									myAgent.addBehaviour(new WakerBehaviour(myAgent,10000){
										protected void handleElapsedTimeout() {

											++SubscriptionBehaviour.this.step;
										}
									});
								}
							}
						}
					}else
						block();
					break;
				case 2:
					//send proposal agreement and start subscription
					ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					msg.addReceiver(((ClientAgent)myAgent).producer.getName());
					msg.setConversationId("Subscription");
					msg.setReplyWith("Subscription"+System.currentTimeMillis());
					myAgent.send(msg);

					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Subscription"),MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
					++step;
					break;
				case 3:
					//wait for subscription acknowledgment
					reply = myAgent.receive(mt);
					if(reply != null){
						if(reply.getPerformative() == ACLMessage.INFORM){
							System.out.println("Client "+myAgent.getName()+" est abonné au Producteur "+reply.getSender());
							++step;
						}
					}
					else{
						block();
					}
				}
			}catch(FIPAException e){
				e.printStackTrace();
			}
		}
	}
}

