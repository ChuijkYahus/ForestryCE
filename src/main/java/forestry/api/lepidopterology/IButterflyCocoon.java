package forestry.api.lepidopterology;

import forestry.api.core.IProduct;
import forestry.api.genetics.alleles.IRegistryAlleleValue;

import java.util.List;

public interface IButterflyCocoon extends IRegistryAlleleValue {
	List<IProduct> getProducts();
}
