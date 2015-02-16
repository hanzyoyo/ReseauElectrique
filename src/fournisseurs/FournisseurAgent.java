package fournisseurs;
import java.util.ArrayList;

import client.ClientAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.introspection.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FournisseurAgent extends Agent{

	private int prixaukilovente;
	private int prixaukiloproduction;
	private int volumerestant;
	private int capital;
	private ArrayList<AID> clients;
	private int demande=0;

	public FournisseurAgent(int prixvente,int prixprod, int volume,int capital){
		this.prixaukilovente=prixvente;
		this.prixaukiloproduction=prixprod;
		this.volumerestant=volume;
		this.capital=capital;
	}




	protected void setup() {

		// Printout a welcome message
		System.out.println("Le fournisseur "+getAID().getName()+" d��marre sa production.");

		// Charger les comportements

		addBehaviour(new OneShotBehaviour(this) {
			//subscribe to DFService as producer of electricity
			@Override
			public void action() {
				// TODO Auto-generated method stub
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());
				ServiceDescription sd =	new	ServiceDescription();
				sd.setType("electricity-producer");
				dfd.addServices(sd);
				try{
					DFService.register(myAgent, dfd);
				}
				catch(FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});

		addBehaviour(new CyclicBehaviour(this) {

			@Override
			public void action() {
				if ((demande>0) && (capital>prixaukiloproduction)){
					((FournisseurAgent) myAgent).produire1kilo();
					for(int i=0 ; i < ((FournisseurAgent) myAgent).clients.size() ; i++){
						//((FournisseurAgent) myAgent).essaivendre(((FournisseurAgent) myAgent).clients.get(i));
					}
				}
				else if (capital<prixaukiloproduction){
					System.out.println("Le fournisseur "+getAID().getName()+" fait faillite.");
					doDelete();
				}
				else{}				
			}
		});
		//...
		//...
	
	 addBehaviour(new CyclicBehaviour){
		 private MessageTemplate mt=null;
		 int step=0;
		 ACLMessage msg=myAgent.receive(mt);
		 switch (step){
		 case 0:
		 if (msg!=null){
			 ACLMessage reply1=msg.createReply(mt);
			 reply1.setPerformative(ACLMessage.PROPOSE);
			 reply1.setContent(String.valueOf(myAgent.prixaukilovente));
			 reply1.send();
			 step++;}
		 
		 else {block();} 
		 break;
			 case 1: 
				 if (msg!=null){
					 if (msg.getPerformative()==REJECT_PROPOSAL){
						 step+=-1;break;}
					 else {myAgent.clients.Add(msg.getSender());
					 ACLMessage reply2=msg.createReply();
					 reply2.setPerformative(ACLMessage.INFORM);
					 reply2.send();step++;}
						 
					 }
			 

		 }
		 
		 }
	 }
	 }

	protected void produire1kilo(){
		this.volumerestant+=1;
		this.capital+=-1;
	}

//	protected void essaivendre(ClientAgent c){
//		if (c.veutELectricite && (c.getCapital()>this.prixaukilovente)){
//			this.volumerestant+=-1;
//			this.capital+=prixaukilovente;
//			c.setCapital(c.getCapital()-this.prixaukilovente);}
//		else {}
//	}

	protected void takeDown() {
		//de-register service
		try{
			DFService.deregister(this);
		}
		catch(FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Le fournisseur "+getAID().getName()+" ne vend plus d'electricit��.");
	}


}