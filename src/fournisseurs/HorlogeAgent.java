package fournisseurs;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class HorlogeAgent extends Agent{
	private int msPerMonth = 20000;

	public void setup(){
		Object[] args = getArguments();

		if(args != null &&	args.length > 0) {
			msPerMonth = (int) args[0];
		}

		addBehaviour(new TickerBehaviour(this,msPerMonth) {

			@Override
			protected void onTick() {
				// TODO send message to all Producers to tell them to charge their customers
				// consult DFService and take first (for now) Producer
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("electricity-producer");
				template.addServices(sd);

				try{
					DFAgentDescription[] results = DFService.search(myAgent, template);
					
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					for(int i = 0; i < results.length; i++){
						msg.addReceiver(results[i].getName());
					}
					msg.setConversationId("top");
					msg.setContent(String.valueOf(this.getTickCount()));
					myAgent.send(msg);
					
					//log
					System.out.println("Agent Horloge envoie un top pour la facturation");
				}catch(FIPAException e){
					e.printStackTrace();
				}
			}
		});

	}
}
