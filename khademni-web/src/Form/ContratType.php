<?php

namespace App\Form;

use App\Entity\Contrat;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ContratType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('clientId', IntegerType::class, [
                'label' => 'ID Client',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('freelancerId', IntegerType::class, [
                'label' => 'ID Freelancer',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('titre', TextType::class, [
                'label' => 'Titre du Contrat',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => false,
                'attr' => ['class' => 'bento-input-textarea', 'rows' => 4]
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('dateContrat', DateType::class, [
                'widget' => 'single_text',
                'label' => 'Date du contrat',
                'attr' => ['class' => 'bento-input']
            ])
            ->add('numTelephone', TextType::class, [
                'label' => 'Téléphone',
                'required' => false,
                'attr' => ['class' => 'bento-input']
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Contrat::class,
        ]);
    }
}
