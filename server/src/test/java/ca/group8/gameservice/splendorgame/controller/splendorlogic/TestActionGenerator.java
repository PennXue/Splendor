package ca.group8.gameservice.splendorgame.controller.splendorlogic;

import ca.group8.gameservice.splendorgame.model.splendormodel.Colour;
import ca.group8.gameservice.splendorgame.model.splendormodel.GameInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TestActionGenerator {

    SplendorActionListGenerator generator;
    GameInfo g1;
    Long gameID = Long.valueOf(123);
    EnumMap<Colour,Integer> price1;
    EnumMap<Colour,Integer> price2;

    @BeforeEach
    void setUp() {
        generator = new SplendorActionListGenerator();
        ArrayList<String> names = new ArrayList<String>();
        names.add("Dave");
        names.add("Bob");
        try {
            g1 = new GameInfo(names);
        } catch (Exception e) {
            System.out.println("TestActionGenerator can't make GameInfo");
        }
        price1 = new EnumMap<>(Colour.class);
        price2 = new EnumMap<>(Colour.class);
        int counter = 1;
        for (Colour colour : Colour.values()) {
            price1.put(colour, 3);
            price2.put(colour, counter);
            counter++;
        }
    }

    @Test
    void firstTurnActions(){
        g1.getCurrentPlayer().getTokenHand().removeToken(price1);
        generator.generateActions(gameID,g1,g1.getCurrentPlayer());
        Map<String, Action> listOfActions = generator.lookUpActions(gameID,g1.getCurrentPlayer().getName());
        assertEquals(listOfActions.size(),13);
    }



}