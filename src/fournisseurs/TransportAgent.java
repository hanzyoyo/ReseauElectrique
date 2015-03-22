package fournisseurs;

import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TransportAgent extends Agent{
	private double prix = 2;

	protected void setup() {

		//inscription au DF
		addBehaviour(new OneShotBehaviour(this) {

			@Override
			public void action() {
				// TODO Auto-generated method stub
				// TODO Auto-generated method stub
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());
				ServiceDescription sd =	new	ServiceDescription();
				sd.setType("electricity-transporter");
				sd.setName("transporter");
				dfd.addServices(sd);
				try{
					DFService.register(myAgent, dfd);
				}
				catch(FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});
	
		addBehaviour(new Behaviour(this) {
			
			@Override
			public boolean done() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public void action() {
				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),MessageTemplate.MatchContent("Demande Prix"));
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null){
					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.INFORM);
					reply.setContent(String.valueOf(prix));
				}else
					block();
				
			}
		});
	}

}
