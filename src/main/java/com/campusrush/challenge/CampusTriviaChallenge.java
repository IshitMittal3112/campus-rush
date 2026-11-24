package com.campusrush.challenge;

import com.campusrush.map.CampusMap;
import com.campusrush.map.Location;
import com.campusrush.map.RouteEdge;
import java.util.*;
public class CampusTriviaChallenge extends MultipleChoiceChallenge {
    private static final long serialVersionUID = 1L;
    public CampusTriviaChallenge(CampusMap map, Random rng) {
        this(pick(map, rng), rng);
    }
    private CampusTriviaChallenge(Setup s, Random rng) {
        super("Campus knowledge: which of these is directly connected to " + s.subject.getName() + " by a path?",
                s.options, 0, rng);
    }
    private static final class Setup {
        Location subject;
        List<String> options;
    }
    private static Setup pick(CampusMap map, Random rng) {
        List<Location> all = new ArrayList<>(map.getAllLocations());
        Location subject = null;
        for (int tries = 0; tries < 50 && subject == null; tries++) {
            Location c = all.get(rng.nextInt(all.size()));
            if (map.getEdgesFrom(c).size() >= 1 && all.size() - map.getEdgesFrom(c).size() - 1 >= 3) subject = c;
        }
        if (subject == null) subject = all.get(0);

        List<RouteEdge> edges = map.getEdgesFrom(subject);
        Set<String> neighbourIds = new HashSet<>();
        for (RouteEdge e : edges) neighbourIds.add(e.getToLocation().getId());

        Location correct = edges.get(rng.nextInt(edges.size())).getToLocation();
        List<Location> others = new ArrayList<>();
        for (Location l : all) {
            if (!l.equals(subject) && !neighbourIds.contains(l.getId())) others.add(l);
        }
        Collections.shuffle(others, rng);

        Setup s = new Setup();
        s.subject = subject;
        s.options = new ArrayList<>();
        s.options.add(correct.getName());          // index 0 = correct (shuffled by base class)
        for (int i = 0; i < 3 && i < others.size(); i++) s.options.add(others.get(i).getName());
        return s;
    }
}
