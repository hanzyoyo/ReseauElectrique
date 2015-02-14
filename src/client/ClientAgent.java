package client;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
		private AID name;
		private int prix = Integer.MAX_VALUE;
		
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
		addBehaviour(new OneShotBehaviour(this) {
			
			@Override
			public void action() {
				// consult DFService and take first (for now) Producer
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("electricity-producer");
				template.addServices(sd);
				
				try{
					DFAgentDescription[] results = DFService.search(myAgent, template);
					//Producer = result[0].getName();
					
					//request price to all electricity producers
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					for(int i = 0; i < results.length; i++){
						msg.addReceiver(results[i].getName());
					}
					msg.setContent("Demande Prix");
					myAgent.send(msg);
				}catch(FIPAException e){
					e.printStackTrace();
				}
			}
		});
		
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
		
		//add cyclic behavior to handle messages
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
					else if(msg.getPerformative() == ACLMessage.INFORM){ //what if message received is an information but not on the prices?
						//keep producer with cheapest price
						if(Integer.valueOf(msg.getContent()) < ((ClientAgent)myAgent).producer.getPrix()){
							((ClientAgent)myAgent).producer.setName(msg.getSender());
							((ClientAgent)myAgent).producer.setPrix(Integer.valueOf(msg.getContent()));
						}
						//TODO : manage timer when message received, if timeout ask for subscription to producer
					}
				}else{
					block();
				}
			}
		});
		
		
	}
}
