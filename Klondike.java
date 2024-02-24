package klondike;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import klondike.card.Card;
import static klondike.card.Card.CARD_HEIGHT;
import static klondike.card.Card.CARD_WIDTH;
import klondike.pile.Foundation;
import klondike.pile.Pile;
import klondike.pile.Tableau;
import static klondike.pile.Tableau.FACE_DOWN;
import static klondike.pile.Tableau.FACE_UP;

public class Klondike extends JFrame {
    final public static String SPADES = "\u2660";
    final public static String HEARTS = "\u2665";
    final public static String CLUBS = "\u2663";
    final public static String DIAMONDS = "\u2666";
    
    final private static Color BACKGROUND = new Color(-16726016);

    final private static int INSET = CARD_WIDTH / 5;
    final private static int GAP = INSET / 9;
    final private static int OVERLAP = INSET * 2;

    final private static int DELAY = 180;

    final private static int TURN_1 = 1;
    final private static int TURN_3 = 3;
    
    final private Pile deck = new Pile();
    final private Pile waste = new Pile();    
    final private Pile[] foundation = {new Foundation(SPADES), new Foundation(HEARTS), new Foundation(CLUBS), new Foundation(DIAMONDS)};
    final private Pile[] tableau = {new Tableau(), new Tableau(), new Tableau(), new Tableau(), new Tableau(), new Tableau(), new Tableau()};
    
    final private ArrayList faceDown = new ArrayList();
    
    private int turnOver = TURN_1;
    
    private Klondike(Card[] cards) {
        super("Klondike");
        
        setIconImage(Toolkit.getDefaultToolkit().createImage("klondike.png"));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Game");
        JPanel table = new JPanel(null);
        JButton auto = new JButton("AUTO");
    
        menuBar.add(menu);

        for (int turnOver : new int[] {TURN_1, TURN_3}) {
            JMenuItem item = new JMenuItem("Turn " + turnOver);
            item.addActionListener(e -> {
                this.turnOver = turnOver;

                auto.setVisible(false);
                auto.setEnabled(true);
            
                faceDown.clear();
            
                deal(Arrays.asList(cards));
            });
            menu.add(item);
        }
       
        table.setPreferredSize(new Dimension(INSET + tableau.length * CARD_WIDTH + (tableau.length - 1) * GAP + INSET, CARD_HEIGHT / 2 + CARD_HEIGHT + INSET + 6 * FACE_DOWN + 11 * FACE_UP + CARD_HEIGHT + INSET));
        table.setBackground(BACKGROUND);
        
        auto.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        auto.setFocusable(false);
        auto.setVisible(false);
        auto.setBounds((table.getPreferredSize().width - (auto.getFontMetrics(auto.getFont()).stringWidth(auto.getText()) + 6)) / 2, (CARD_HEIGHT / 2 - (auto.getFont().getSize() + 6)) / 2, auto.getFontMetrics(auto.getFont()).stringWidth(auto.getText()) + 6, auto.getFont().getSize() + 6);
        auto.addActionListener(e -> {
            auto.setEnabled(false);
            
            new Thread() {
                @Override
                public void run() {
                    menu.setEnabled(false);
                            
                    do {
                        for (Pile pile : tableau) {
                            if (!pile.isEmpty()) {
                                Card card = pile.getCard();
                                Pile suit = foundation[Card.SUIT.indexOf(card.getSuit())];
                                
                                if (card.getRank() == Card.RANK[suit.getComponentCount()]) {
                                    suit.setCard(card);
                                    
                                    repaint();
                                    
                                    try {
                                        Thread.sleep(DELAY);
                                    } catch (Exception ex) { }
                                }
                            }
                        }
                    } while (!isFinished());
                    
                    menu.setEnabled(true);
                }
            }.start();
        });
        table.add(auto);
        
        for (int i = 0; i < foundation.length; i++) {
            foundation[i].setBounds(INSET + i * (CARD_WIDTH + GAP), CARD_HEIGHT / 2, CARD_WIDTH, CARD_HEIGHT);
            table.add(foundation[i]);
        }
        
        waste.setBounds(INSET + 5 * (CARD_WIDTH + GAP) - 2 * OVERLAP, CARD_HEIGHT / 2, 2 * OVERLAP + CARD_WIDTH, CARD_HEIGHT);
        waste.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                ((Card) e.getChild()).setFaceUp(true);
                e.getChild().setLocation(waste.getWidth() - e.getChild().getWidth(), 0);
                
                for (int i = 1; i < Math.min(3, waste.getComponentCount()); i++) {
                    waste.getComponent(i).setLocation(waste.getComponent(i).getX() - OVERLAP, waste.getComponent(i).getY());
                }
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                for (int i = 0; i < Math.min(2, waste.getComponentCount()); i++) {
                    waste.getComponent(i).setLocation(waste.getComponent(i).getX() + OVERLAP, waste.getComponent(i).getY());
                }
            }
        });
        table.add(waste);

        deck.setBounds(INSET + 6 * (CARD_WIDTH + GAP), CARD_HEIGHT / 2, CARD_WIDTH, CARD_HEIGHT);
        deck.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                ((Card) e.getChild()).setFaceUp(false);
                e.getChild().setLocation(0, 0);
            }
        });
        table.add(deck);
        
        for (int i = 0; i < tableau.length; i++) {
            tableau[i].setBounds(INSET + i * (CARD_WIDTH + GAP), CARD_HEIGHT / 2 + CARD_HEIGHT + INSET, CARD_WIDTH, i * FACE_DOWN + 11 * FACE_UP + CARD_HEIGHT);
            table.add(tableau[i]);
        }
        
        table.addMouseListener(new MouseAdapter() {
            boolean isLegal(Pile tableau, Card card) {
                if (card.getRank() == Card.KING) {
                    return tableau.isEmpty();
                } else {
                    return card.getRank() != Card.ACE && !tableau.isEmpty() && tableau.getCard().isFaceUp() && tableau.getCard().getForeground() != card.getForeground() && tableau.getCard().getRank() == Card.RANK[Arrays.asList(Card.RANK).indexOf(card.getRank()) + 1];
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (auto.isEnabled()) {
                    try {
                        Card card = (Card) table.findComponentAt(e.getPoint());
                        Pile pressed = (Pile) card.getParent();

                        if (pressed instanceof Foundation || (pressed == waste && card == waste.getCard()) || (pressed instanceof Tableau && card.isFaceUp())) {
                            Pile next = null;
                        
                            for (int i = Arrays.asList(tableau).indexOf(pressed) + 1; i < tableau.length; i++) {
                                if (isLegal(tableau[i], card)) {
                                    next = tableau[i];
                                    break;
                                }
                            }

                            if (next == null && (pressed == waste || (pressed instanceof Tableau && card == pressed.getCard()))) {
                                Pile suit = foundation[Card.SUIT.indexOf(card.getSuit())];
                                
                                if (card.getRank() == Card.RANK[suit.getComponentCount()]) {
                                    next = suit;
                                }
                            }
                            
                            if (next == null && pressed instanceof Tableau) {
                                for (int i = 0; tableau[i] != pressed; i++) {
                                    if (isLegal(tableau[i], card)) {
                                        next = tableau[i];
                                        break;
                                    }
                                }
                            }
                            
                            if (next != null) {
                                for (int i = pressed.getComponentZOrder(card); i >= 0; i--) {
                                    next.setCard((Card) pressed.getComponent(i));
                                }
                                
                                if (isFinished()) {
                                    auto.setEnabled(false);
                                } else if (pressed == waste && waste.isEmpty() && deck.isEmpty()) {
                                    auto.setVisible(faceDown.isEmpty());
                                }
                            }
                        } else if (pressed instanceof Tableau && card == pressed.getCard()) {
                            card.setFaceUp(true);
                            
                            if (faceDown.remove(card) && faceDown.isEmpty()) {
                                auto.setVisible(waste.isEmpty() && deck.isEmpty());
                            }
                        }
                        
                        repaint();
                    } catch (Exception ex) { }
                }
            }
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (!deck.isEmpty()) {
                        for (int min = Math.min(turnOver, deck.getComponentCount()), i = 0; i < min; i++) {
                            waste.setCard(deck.getCard());
                        }
                    } else if (!waste.isEmpty()) {
                        do {
                            deck.setCard(waste.getCard());
                        } while (!waste.isEmpty());
                    }
                    
                    repaint();
                }
            }
        });
        
        setJMenuBar(menuBar);
        setContentPane(table);
        pack();
        setVisible(true);
        setLocationRelativeTo(null);
        
        deal(Arrays.asList(cards));
    }
    
    private void deal(List<Card> cards) {
        Collections.shuffle(cards);
        cards.forEach(card -> deck.setCard(card));
        
        for (int i = 0; i < tableau.length; i++) {
            do {
                tableau[i].setCard(deck.getCard());
            } while (tableau[i].getComponentCount() <= i);
            
            tableau[i].getCard().setFaceUp(true);
        }

        for (int i = 1; i < tableau.length; i++) {
            faceDown.add(tableau[i].getComponent(i));
        }
        
        repaint();
    }
    
    private boolean isFinished() {
        for (Pile suit : foundation) {
            if (suit.getComponentCount() < Card.RANK.length) {
                return false;
            }
        }
        
        return true;
    }
    
    public static void main(String[] args) {
        new Klondike(Card.getCards());
    }
    
}
