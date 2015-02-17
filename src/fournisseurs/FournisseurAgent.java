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
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FournisseurAgent extends Agent{

	private int prixaukilovente = 10;
	private int prixaukiloproduction;
	private int volumerestant;
	private int capital;
	private ArrayList<AID> clients = new ArrayList<AID>();
	private int demande=0;

	/*public FournisseurAgent(int prixvente,int prixprod, int volume,int capital){
		this.prixaukilovente=prixvente;
		this.prixaukiloproduction=prixprod;
		this.volumerestant=volume;
		this.capital=capital;
	}*/


	protected void setup() {

		// Printout a welcome message
		System.out.println("Le fournisseur "+getAID().getName()+" démarre sa production.");

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
				sd.setName("production");
				dfd.addServices(sd);
				try{
					DFService.register(myAgent, dfd);
				}
				catch(FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});

		/*		addBehaviour(new CyclicBehaviour(this) {

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
		});*/

		addBehaviour(new CyclicBehaviour(this){
			private int step = 0;
			public void action() {
				FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;
				ACLMessage msg;
				MessageTemplate mt;

				switch (step){
				case 0:
					mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					msg=myAgent.receive(mt);
					if (msg!=null){							
						ACLMessage reply1=msg.createReply();
						reply1.setPerformative(ACLMessage.PROPOSE);
						reply1.setContent(String.valueOf(myFournisseur.prixaukilovente));
						myAgent.send(reply1);

						//log
						System.out.println("Producteur " + myAgent.getName() + " envoie proposition au Client " + (AID)reply1.getAllReceiver().next());

						step++;
					}
					else {
						block();
					} 
					break;
				case 1: 
					//need to make a template to limit search to proposal response
					msg=myAgent.receive();
					if (msg!=null){
						if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL){

							//log
							System.out.println("Agent " + msg.getSender() + " refuse la proposition.");

							step+=-1;
							break;
						}
						else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
							myFournisseur.clients.add(msg.getSender());
							ACLMessage reply2=msg.createReply();
							reply2.setPerformative(ACLMessage.INFORM);
							myAgent.send(reply2);

							//log
							System.out.println("Producteur " + myAgent.getName() + " confirme l'abonnement du Client " + msg.getAllReceiver());

							step = 0;
						}

					}
					break;
				}
			}
		});

		addBehaviour(new CyclicBehaviour(this){
			public void action(){
				FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;

				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchConversationId("top"));
				ACLMessage msg=myAgent.receive(mt);

				if (msg!=null){
					//log
					System.out.println("Producteur "+myAgent.getName()+" a reçu un top");

					ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
					for (int i=0;i<myFournisseur.clients.size();i++){
						req.addReceiver(myFournisseur.clients.get(i));
					}
					myFournisseur.send(req);

					//log
					System.out.println("Producteur "+myFournisseur.getName()+" a envoyé les demandes de consommation");
				}
				else {
					block();
				}
			}
		});

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
