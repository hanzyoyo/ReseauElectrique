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
	
	private AID Producer;

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
					DFAgentDescription[] result = DFService.search(myAgent, template);
					Producer = result[0].getName();
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
		
		//add cyclic behavior to transmit consumption to producer when requested by producer
		addBehaviour(new CyclicBehaviour(this) {
			
			@Override
			public void action() {
				// if request received
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = myAgent.receive(mt);
				
				if(msg != null){
					//if received request is from the agent's producer 
					if(msg.getSender() == ((ClientAgent)myAgent).Producer){
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(String.valueOf(monthlyTotal));
						myAgent.send(reply);
					}
				}else{
					block();
				}
			}
		});
		
		
	}
}
