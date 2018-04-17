package jasmine.algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapSorting {

	// sort a map based on its value, the first item of the map being the one with the highest value
	public static <K, V extends Comparable<? super V>> Map<K, V>sortByValueDescending( Map<K, V> map ){
		List<Map.Entry<K, V>> list =  new LinkedList<>( map.entrySet() );
		Collections.sort( list, new Comparator<Map.Entry<K, V>>(){
        
			@Override
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ){
				return ( o2.getValue() ).compareTo( o1.getValue() );
				}
			} 
		);

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put( entry.getKey(), entry.getValue() );
		}
		return result;
	} 
	
	// sort a map based on its value, the first item of the map being the one with the lowest value
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending( Map<K, V> map ) {
	    List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
	    Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
	    	
	    	@Override
	        public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
	            return (o1.getValue()).compareTo( o2.getValue() );
	        	}
	    	} 
	    );
	
	    Map<K, V> result = new LinkedHashMap<K, V>();
	    for (Map.Entry<K, V> entry : list){
	        result.put( entry.getKey(), entry.getValue() );
	    }
	    return result;
	}
	
}
