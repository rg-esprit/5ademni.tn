<?php

namespace App\Form;

use App\Entity\Payment;
use App\Entity\Contrat;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class PaymentType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('contrat', EntityType::class, [
                'class' => Contrat::class,
                'choice_label' => 'titre',
                'label' => 'Contrat',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('amount', NumberType::class, [
                'label' => 'Montant (TND)',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('status', ChoiceType::class, [
                'choices' => [
                    'Payé' => 'PAID',
                    'En attente' => 'PENDING',
                    'Annulé' => 'CANCELLED',
                ],
                'label' => 'Statut',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('stripeSessionId', TextType::class, [
                'label' => 'ID Session Stripe',
                'required' => false,
                'attr' => ['class' => 'bento-input']
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Payment::class,
        ]);
    }
}
