package gui;

import jade.util.leap.ArrayList;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import client, fournisseurs;

public class GUI {
	public static void main(String[] args) {
		ArrayList<ClientAgent> clients = new ArrayList<ClientAgent>;
		
	}
	
	public void instantiateGUI(){
		JFrame fenetre2 = new JFrame("Premier contenu");
		fenetre2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre2.setPreferredSize(new Dimension(600, 400));

		fenetre2.setLayout(new GridLayout(2, 2));

		fenetre2.add(new JLabel("Texte1"));
		fenetre2.add(new JTextArea("Vous pouvez modifier ce texte",4,15));
		fenetre2.add(new JCheckBox("cochez moi !"));
		fenetre2.add(new JButton("clic ?"));
		fenetre2.add(new JLabel("Texte2"));
		JTextArea t = new JTextArea("Vous ne pouvez pas modifier celui-ci",4,15);
		t.setEditable(false);
		fenetre2.add(t);

		fenetre2.pack();
		fenetre2.setVisible(true);
	}

}
