package com.wbc.core.jalo.promotions;

import de.hybris.platform.core.CoreAlgorithms;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.c2l.Currency;
import de.hybris.platform.jalo.order.AbstractOrderEntry;
import de.hybris.platform.jalo.order.OrderManager;
import de.hybris.platform.jalo.order.price.JaloPriceFactoryException;
import de.hybris.platform.promotions.jalo.PromotionOrderEntryConsumed;
import de.hybris.platform.promotions.jalo.PromotionResult;
import de.hybris.platform.promotions.result.PromotionEvaluationContext;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.wbc.core.price.factory.WBCPriceFactory;


public class ProductFixedPricePromotion extends de.hybris.platform.promotions.jalo.ProductFixedPricePromotion
{
	private static final Logger LOG = Logger.getLogger(ProductFixedPricePromotion.class.getName());

	@Override
	public List<PromotionResult> evaluate(final SessionContext arg0, final PromotionEvaluationContext arg1)
	{
		final List<PromotionResult> promotionResults = super.evaluate(arg0, arg1);
		if (promotionResults != null)
		{
			final WBCPriceFactory wbcPricefactory = (WBCPriceFactory) OrderManager.getInstance().getPriceFactory();
			final Currency curr = arg1.getOrder().getCurrency();
			final int digits = curr.getDigits().intValue();
			final List validPromotionResults = new ArrayList();
			for (final PromotionResult promotionResult : promotionResults)
			{
				for (final PromotionOrderEntryConsumed promotionOrderEntryConsumed : promotionResult.getConsumedEntries())
				{
					if (isRestrictedProduct(promotionOrderEntryConsumed.getOrderEntry(),
							promotionOrderEntryConsumed.getAdjustedUnitPriceAsPrimitive(), wbcPricefactory, digits, curr))
					{
						break;
					}
					if (!validPromotionResults.contains(promotionResult))
					{
						validPromotionResults.add(promotionResult);
					}
				}
			}
			return validPromotionResults;
		}
		return promotionResults;
	}

	private boolean isRestrictedProduct(final AbstractOrderEntry orderEntry, final double adjustedSiebelBasePrice,
			final WBCPriceFactory wbcPricefactory, final int digits, final Currency curr)
	{
		final double siebelBasePrice = orderEntry.getBasePriceAsPrimitive();
		try
		{
			final double retailBasePrice = wbcPricefactory.getRetailPrice(orderEntry).getValue();
			final double retailTotalPrice = CoreAlgorithms.round(retailBasePrice * orderEntry.getQuantity().longValue(), digits);

			if (siebelBasePrice < adjustedSiebelBasePrice)
			{
				return true;
			}
			else
			{
				LOG.debug("ProductFixedPricePromotion is being applied on retailprice of " + retailTotalPrice + " for product "
						+ orderEntry.getProduct().getCode());
				orderEntry.setBasePrice(retailBasePrice);
				orderEntry.setTotalPrice(retailTotalPrice);
			}
		}
		catch (final JaloPriceFactoryException e)
		{
			LOG.error("Error in evaluating ProductFixedPromotion ", e);
		}

		return false;
	}
}